package com.donator.fun420;

import com.google.inject.Provides;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Random;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Donator - Fun 420",
	description = "Visual 4:20 alarm, an April 20 banner and a timed 420 quiz",
	tags = {"420", "fun", "alarm", "banner", "quiz", "overlay"}
)
public class Fun420Plugin extends Plugin
{
	/** The value a boolean config item carries once the player switches it on. */
	private static final String CONFIG_TRUE = "true";

	@Inject
	private Client client;

	@Inject
	private Fun420Config config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AlarmOverlay alarmOverlay;

	@Inject
	private BannerOverlay bannerOverlay;

	@Inject
	private IntroOverlay introOverlay;

	@Inject
	private QuizOverlay quizOverlay;

	@Inject
	private BannerMouseListener bannerMouseListener;

	@Getter
	private final Fun420Clock clock = new Fun420Clock();

	@Getter
	private final AlarmState alarmState = new AlarmState();

	@Getter
	private final IntroState introState = new IntroState();

	@Getter
	private final QuizState quizState = new QuizState();

	private final ArrivalDetector arrivalDetector = new ArrivalDetector();

	private final QuizSchedule quizSchedule = new QuizSchedule();

	/**
	 * Plainly seeded: which question comes up is meant to be unpredictable,
	 * and nothing here has to be reproducible between clients. Tests that need
	 * a fixed order build their own deck.
	 */
	private final QuizDeck quizDeck = new QuizDeck(QuizQuestions.ALL, new Random());

	/**
	 * The date on which the player clicked the banner away, or null if never.
	 * Volatile because the dismissal arrives on another thread than the one
	 * that renders, matching {@link AlarmState} and {@link Fun420Clock}.
	 */
	private volatile LocalDate bannerDismissedOn;

	public boolean isBannerVisible()
	{
		return BannerState.isVisible(clock.now().toLocalDate(), bannerDismissedOn);
	}

	/**
	 * Closes the banner for the day it is currently showing. A dismissal taken
	 * while the April 20 simulation is on is deliberately not written to the
	 * config: the clock reports the simulated April 20, so persisting it would
	 * suppress the banner on the real April 20 that was being previewed.
	 */
	void dismissBanner()
	{
		LocalDate today = clock.now().toLocalDate();
		bannerDismissedOn = today;

		if (config.simulateApril20())
		{
			// Skipping the write costs a dismissal that does not survive a
			// restart when it really is April 20 and the preview happens to be
			// on. That is the accepted trade: the alternative silently kills
			// the real banner, so do not "fix" this by always writing.
			log.debug("420 banner preview closed for {}, not stored while simulating", today);
			return;
		}

		configManager.setConfiguration(
			Fun420Config.GROUP,
			Fun420Config.KEY_BANNER_DISMISSED_ON,
			today.toString());
		log.debug("420 banner dismissed for {}", today);
	}

	@Override
	protected void startUp()
	{
		// Loads bannerDismissedOn as well; see applySimulation.
		applySimulation();
		// Taken from the live state instead of assumed: enabling the plugin
		// while already logged in must not make the next region load look like
		// an arrival.
		arrivalDetector.seed(client.getGameState());
		overlayManager.add(alarmOverlay);
		overlayManager.add(bannerOverlay);
		overlayManager.add(introOverlay);
		overlayManager.add(quizOverlay);
		mouseManager.registerMouseListener(bannerMouseListener);
		log.debug("Donator - Fun 420 started");
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(bannerMouseListener);
		overlayManager.remove(alarmOverlay);
		overlayManager.remove(bannerOverlay);
		overlayManager.remove(introOverlay);
		overlayManager.remove(quizOverlay);
		alarmState.reset();
		introState.reset();
		quizState.reset();
		quizSchedule.reset();
		arrivalDetector.reset();
		clock.useRealDate();
		log.debug("Donator - Fun 420 stopped");
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		boolean inGame = isInGame();
		alarmState.update(clock.now(), inGame);
		updateQuiz(inGame);
	}

	/**
	 * Puts a question on screen when one is due. Everything it asks is asked
	 * of somebody else: whether a box is up is {@link QuizState}'s answer and
	 * whether the wait is over is {@link QuizSchedule}'s.
	 * <p>
	 * Runs on the real calendar rather than on {@link Fun420Clock#now()}: the
	 * April 20 preview moves that clock by months, and a run measured from a
	 * moment on the simulated calendar would end the instant the preview was
	 * switched on or off.
	 */
	private void updateQuiz(boolean inGame)
	{
		if (!inGame || !config.quizEnabled())
		{
			// The wait only runs while there is something to wait for, so
			// switching the quiz off stops the timer. Switching it back on
			// then costs a whole interval instead of putting a question on
			// screen on the spot for the time it was off.
			quizSchedule.reset();
			return;
		}

		LocalDateTime now = clock.realNow();

		if (quizState.isActive(now))
		{
			// The wait for the next question begins when this one is gone, so
			// the timer is held at the current moment for as long as a box is
			// on screen. That also settles what "two runs at once" would mean:
			// there is no path to a second one while the first is up.
			quizSchedule.restart(now);
			return;
		}

		// Starts the wait on the first tick in the game and leaves a running
		// one alone, so that the first question of a session is a full
		// interval away rather than immediate.
		quizSchedule.startIfNotRunning(now);

		if (quizSchedule.isDue(now, Duration.ofMinutes(config.quizIntervalMinutes())))
		{
			startQuiz(now);
		}
	}

	/**
	 * Deals a question and puts it on screen, whatever the timer says.
	 * <p>
	 * Client thread only, because {@link QuizDeck#draw()} is.
	 */
	private void startQuiz(LocalDateTime now)
	{
		quizState.start(
			now,
			quizDeck.draw(),
			Duration.ofSeconds(config.quizThinkingSeconds()),
			Duration.ofSeconds(config.quizAnswerSeconds()));
		// The wait restarts here as well as on every tick the box is up, so
		// that a run which somehow saw no tick at all still costs an interval.
		quizSchedule.restart(now);
		log.debug("420 quiz question shown at {}", now);
	}

	/**
	 * Routes a state change: leaving the game clears what is on screen, and
	 * arriving in it starts the opening message. Which state changes count as
	 * arriving is {@link ArrivalDetector}'s judgement, not this method's.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();

		if (!ArrivalDetector.isInGame(state))
		{
			alarmState.reset();
			introState.reset();
			quizState.reset();
			// Stops the wait as well as the box: a timer that kept running
			// while logged out would greet the next login with a question, and
			// after a night away with one every interval it had "missed".
			quizSchedule.reset();
		}

		if (arrivalDetector.onState(state))
		{
			startIntro();
		}
	}

	/**
	 * Shows the opening message if the calendar says April 20. Deliberately
	 * blind to the setting that hides it: the state stays a plain record of
	 * what happened and the overlay decides whether it is drawn.
	 */
	private void startIntro()
	{
		LocalDateTime now = clock.now();
		if (Fun420Clock.isBannerDay(now.toLocalDate()))
		{
			introState.start(now);
		}
	}

	private boolean isInGame()
	{
		return ArrivalDetector.isInGame(client.getGameState());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!Fun420Config.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (Fun420Config.KEY_TEST_ALARM.equals(event.getKey()))
		{
			// Judged on the value this event carries rather than on a config
			// read: switching the item back off below posts a second event for
			// the same key, and that one carries "false", so it cannot loop.
			if (CONFIG_TRUE.equals(event.getNewValue()))
			{
				alarmState.start(clock.now());
				configManager.setConfiguration(
					Fun420Config.GROUP,
					Fun420Config.KEY_TEST_ALARM,
					false);
			}
			return;
		}

		if (Fun420Config.KEY_TEST_QUIZ.equals(event.getKey()))
		{
			// Same edge trigger as the alarm above: judged on the value this
			// event carries, so that switching the item back off cannot loop.
			if (CONFIG_TRUE.equals(event.getNewValue()))
			{
				// Handed to the client thread because the deck deals there
				// only, while a switch in the settings panel is flipped on the
				// thread that draws that panel.
				clientThread.invoke(() -> startQuiz(clock.realNow()));
				configManager.setConfiguration(
					Fun420Config.GROUP,
					Fun420Config.KEY_TEST_QUIZ,
					false);
			}
			return;
		}

		if (Fun420Config.KEY_SIMULATE_APRIL_20.equals(event.getKey()))
		{
			applySimulation();
			if (config.simulateApril20())
			{
				// Switching the preview on stands in for a login, so that the
				// opening message can be judged without leaving the game.
				startIntro();
			}
		}

		// Any other key of this group, including the hidden dismissal date the
		// plugin writes itself, needs no handling.
	}

	/**
	 * Puts the clock on the simulated or the real calendar. Sole owner of
	 * {@link #bannerDismissedOn}, which is why it writes the field on both
	 * branches instead of leaving one of them to a caller.
	 */
	private void applySimulation()
	{
		if (config.simulateApril20())
		{
			clock.simulateApril20();
			// In-memory in both directions: a stored dismissal for the real
			// April 20 must not blank the preview it is meant to show.
			bannerDismissedOn = null;
			return;
		}

		clock.useRealDate();
		// Back on the real calendar, so drop a dismissal that was only taken to
		// close the preview and was never stored. A real dismissal was stored
		// and comes straight back.
		bannerDismissedOn = readDismissedDate();
		// A running preview of the opening message needs no reset here. Its
		// start moment was taken from the simulated clock and therefore sits on
		// April 20, so against the real calendar it is either months in the
		// past or months in the future: both read as inactive at once. Only on
		// the real April 20 do the two calendars agree, and there the message
		// is meant to keep running anyway.
	}

	private LocalDate readDismissedDate()
	{
		String raw = configManager.getConfiguration(
			Fun420Config.GROUP,
			Fun420Config.KEY_BANNER_DISMISSED_ON);

		if (raw == null || raw.isEmpty())
		{
			return null;
		}

		try
		{
			return LocalDate.parse(raw);
		}
		catch (DateTimeParseException e)
		{
			log.debug("Unreadable stored date: {}", raw);
			return null;
		}
	}

	@Provides
	Fun420Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(Fun420Config.class);
	}
}
