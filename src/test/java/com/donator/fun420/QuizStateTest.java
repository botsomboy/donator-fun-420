package com.donator.fun420;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuizStateTest
{
	private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 14, 5, 0);

	private static final QuizQuestion QUESTION =
		new QuizQuestion("What is 420?", "A time and a code.");
	private static final QuizQuestion OTHER_QUESTION =
		new QuizQuestion("What is hemp?", "Cannabis grown for fibre.");

	/** The presentation constants the box slides and fades with. */
	private static final long SLIDE_MILLIS = 400L;
	private static final long FADE_MILLIS = 600L;

	private static final Duration THINKING = Duration.ofSeconds(10);
	private static final Duration ANSWER = Duration.ofSeconds(10);

	/** Slide, thinking and answer are behind us; the fade begins here. */
	private static final long FADE_START = SLIDE_MILLIS + 20_000L;
	private static final long TOTAL = FADE_START + FADE_MILLIS;

	private static LocalDateTime at(long millis)
	{
		return START.plus(Duration.ofMillis(millis));
	}

	private static QuizState started()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, THINKING, ANSWER);
		return state;
	}

	// --- being on screen at all ------------------------------------------

	@Test
	public void isInactiveInitially()
	{
		assertFalse(new QuizState().isActive(START));
	}

	@Test
	public void hasNoQuestionInitially()
	{
		assertNull(new QuizState().getQuestion());
	}

	@Test
	public void isActiveFromTheStartMoment()
	{
		assertTrue(started().isActive(START));
	}

	@Test
	public void carriesTheQuestionItWasStartedWith()
	{
		assertSame(QUESTION, started().getQuestion());
	}

	@Test
	public void runsUntilTheFadeIsDone()
	{
		QuizState state = started();

		assertTrue("still fading just before the end", state.isActive(at(TOTAL - 1)));
		assertFalse("gone the moment the fade is done", state.isActive(at(TOTAL)));
	}

	@Test
	public void staysGoneLongAfterTheFade()
	{
		assertFalse(started().isActive(at(10 * TOTAL)));
	}

	@Test
	public void isInactiveWhenTheClockJumpsBackwards()
	{
		assertFalse(started().isActive(START.minusHours(1)));
	}

	@Test
	public void resetHidesItImmediately()
	{
		QuizState state = started();
		state.reset();

		assertFalse(state.isActive(at(1000)));
		assertNull(state.getQuestion());
		assertEquals(0.0, state.slideProgress(at(1000)), 0.0001);
		assertEquals(0.0, state.thinkingRemaining(at(1000)), 0.0001);
		assertFalse(state.answerVisible(at(1000)));
		assertEquals(0.0, state.opacity(at(1000)), 0.0001);
	}

	// --- sliding in -------------------------------------------------------

	@Test
	public void slideProgressStartsAtZero()
	{
		assertEquals(0.0, started().slideProgress(START), 0.0001);
	}

	@Test
	public void slideProgressRunsLinearlyThroughTheSlide()
	{
		QuizState state = started();

		assertEquals(0.25, state.slideProgress(at(100)), 0.0001);
		assertEquals(0.5, state.slideProgress(at(200)), 0.0001);
		assertEquals(0.75, state.slideProgress(at(300)), 0.0001);
		assertEquals(0.9975, state.slideProgress(at(399)), 0.0001);
	}

	@Test
	public void slideProgressReachesOneWhenTheSlideEndsAndStaysThere()
	{
		QuizState state = started();

		assertEquals(1.0, state.slideProgress(at(SLIDE_MILLIS)), 0.0001);
		assertEquals(1.0, state.slideProgress(at(SLIDE_MILLIS + 1)), 0.0001);
		assertEquals(1.0, state.slideProgress(at(5000)), 0.0001);
		assertEquals(1.0, state.slideProgress(at(TOTAL - 1)), 0.0001);
	}

	/** Kills a slide that jumps rather than moves: every step must be larger. */
	@Test
	public void slideProgressKeepsRisingThroughTheSlide()
	{
		QuizState state = started();

		double previous = state.slideProgress(START);
		for (long millis = 10; millis < SLIDE_MILLIS; millis += 10)
		{
			double current = state.slideProgress(at(millis));
			assertTrue("the box should have moved on at " + millis + "ms,"
					+ " was " + previous + " and is " + current,
				current > previous);
			previous = current;
		}
	}

	@Test
	public void slideProgressIsZeroWhenNothingIsOnScreen()
	{
		QuizState state = started();

		assertEquals(0.0, new QuizState().slideProgress(START), 0.0001);
		assertEquals(0.0, state.slideProgress(START.minusHours(1)), 0.0001);
		assertEquals(0.0, state.slideProgress(at(TOTAL)), 0.0001);
	}

	@Test
	public void slideProgressStaysBetweenZeroAndOne()
	{
		QuizState state = started();

		for (long millis = -1000; millis <= TOTAL + 1000; millis += 7)
		{
			double progress = state.slideProgress(at(millis));
			assertTrue("slide progress too low at " + millis + "ms", progress >= 0.0);
			assertTrue("slide progress too high at " + millis + "ms", progress <= 1.0);
		}
	}

	// --- thinking time ----------------------------------------------------

	@Test
	public void thinkingRemainingIsFullWhileTheBoxSlidesIn()
	{
		QuizState state = started();

		// Sampled off the whole seconds as well: a bar that drains during the
		// slide only shows up between the ticks.
		for (long millis = 0; millis < SLIDE_MILLIS; millis += 13)
		{
			assertEquals("the bar should still be full at " + millis + "ms",
				1.0, state.thinkingRemaining(at(millis)), 0.0001);
		}
	}

	@Test
	public void thinkingRemainingIsFullWhenTheThinkingBegins()
	{
		assertEquals(1.0, started().thinkingRemaining(at(SLIDE_MILLIS)), 0.0001);
	}

	@Test
	public void thinkingRemainingDrainsLinearlyOverTheThinkingTime()
	{
		QuizState state = started();

		assertEquals(0.75, state.thinkingRemaining(at(SLIDE_MILLIS + 2500)), 0.0001);
		assertEquals(0.5, state.thinkingRemaining(at(SLIDE_MILLIS + 5000)), 0.0001);
		assertEquals(0.25, state.thinkingRemaining(at(SLIDE_MILLIS + 7500)), 0.0001);
		assertEquals(0.01, state.thinkingRemaining(at(SLIDE_MILLIS + 9900)), 0.0001);
	}

	/**
	 * Both sides of the moment the bar empties. Asserting only that it reads
	 * zero at the boundary would hold just as well for a bar that emptied a
	 * millisecond early, so the millisecond before it has to be above zero.
	 */
	@Test
	public void thinkingRemainingReachesZeroExactlyWhenTheThinkingEnds()
	{
		QuizState state = started();

		assertTrue("the bar should not be empty yet one millisecond earlier",
			state.thinkingRemaining(at(SLIDE_MILLIS + 9999)) > 0.0);
		assertEquals(0.0, state.thinkingRemaining(at(SLIDE_MILLIS + 10_000)), 0.0);
	}

	@Test
	public void thinkingRemainingIsEmptyOnceTheThinkingIsOver()
	{
		QuizState state = started();

		assertEquals(0.0, state.thinkingRemaining(at(SLIDE_MILLIS + 10_000)), 0.0001);
		assertEquals(0.0, state.thinkingRemaining(at(SLIDE_MILLIS + 15_000)), 0.0001);
		assertEquals(0.0, state.thinkingRemaining(at(FADE_START + 100)), 0.0001);
	}

	/** Kills a bar that stands still and empties at the last moment. */
	@Test
	public void thinkingRemainingKeepsFallingThroughTheThinking()
	{
		QuizState state = started();

		double previous = state.thinkingRemaining(at(SLIDE_MILLIS));
		for (long millis = SLIDE_MILLIS + 50; millis < SLIDE_MILLIS + 10_000; millis += 50)
		{
			double current = state.thinkingRemaining(at(millis));
			assertTrue("the bar should have drained further at " + millis + "ms,"
					+ " was " + previous + " and is " + current,
				current < previous);
			previous = current;
		}
	}

	@Test
	public void thinkingRemainingIsEmptyWhenNothingIsOnScreen()
	{
		QuizState state = started();

		assertEquals(0.0, new QuizState().thinkingRemaining(START), 0.0001);
		assertEquals(0.0, state.thinkingRemaining(START.minusHours(1)), 0.0001);
		assertEquals(0.0, state.thinkingRemaining(at(TOTAL)), 0.0001);
	}

	@Test
	public void thinkingRemainingStaysBetweenZeroAndOne()
	{
		QuizState state = started();

		for (long millis = -1000; millis <= TOTAL + 1000; millis += 7)
		{
			double remaining = state.thinkingRemaining(at(millis));
			assertTrue("thinking remaining too low at " + millis + "ms", remaining >= 0.0);
			assertTrue("thinking remaining too high at " + millis + "ms", remaining <= 1.0);
		}
	}

	// --- the answer -------------------------------------------------------

	@Test
	public void answerIsHiddenWhileTheBoxSlidesInAndWhileThinking()
	{
		QuizState state = started();

		assertFalse(state.answerVisible(START));
		assertFalse(state.answerVisible(at(SLIDE_MILLIS - 1)));
		assertFalse(state.answerVisible(at(SLIDE_MILLIS)));
		assertFalse(state.answerVisible(at(SLIDE_MILLIS + 5000)));
	}

	@Test
	public void answerAppearsTheMomentTheThinkingRunsOut()
	{
		QuizState state = started();

		assertFalse("still thinking one millisecond before the bar is empty",
			state.answerVisible(at(SLIDE_MILLIS + 9999)));
		assertTrue("shown the moment the bar is empty",
			state.answerVisible(at(SLIDE_MILLIS + 10_000)));
	}

	@Test
	public void answerStaysVisibleWhileTheBoxFadesOut()
	{
		QuizState state = started();

		assertTrue(state.answerVisible(at(FADE_START - 1)));
		assertTrue(state.answerVisible(at(FADE_START)));
		assertTrue(state.answerVisible(at(TOTAL - 1)));
	}

	@Test
	public void answerIsHiddenWhenNothingIsOnScreen()
	{
		QuizState state = started();

		assertFalse(new QuizState().answerVisible(START));
		assertFalse(state.answerVisible(START.minusHours(1)));
		assertFalse(state.answerVisible(at(TOTAL)));
	}

	// --- fading out -------------------------------------------------------

	@Test
	public void opacityIsFullUntilTheFadeBegins()
	{
		QuizState state = started();

		for (long millis = 0; millis <= FADE_START; millis += 37)
		{
			assertEquals("opacity should still be full at " + millis + "ms",
				1.0, state.opacity(at(millis)), 0.0001);
		}
		assertEquals(1.0, state.opacity(at(FADE_START)), 0.0001);
	}

	@Test
	public void opacityFallsLinearlyThroughTheFade()
	{
		QuizState state = started();

		assertEquals(0.75, state.opacity(at(FADE_START + 150)), 0.0001);
		assertEquals(0.5, state.opacity(at(FADE_START + 300)), 0.0001);
		assertEquals(0.25, state.opacity(at(FADE_START + 450)), 0.0001);
		assertEquals(0.1, state.opacity(at(FADE_START + 540)), 0.0001);
	}

	/**
	 * Both sides of the moment the fade begins. Full opacity at the boundary
	 * holds just as well for a fade that starts a millisecond later, so the
	 * millisecond after it has to be below full.
	 */
	@Test
	public void opacityStartsFallingExactlyWhenTheAnswerTimeIsUp()
	{
		QuizState state = started();

		assertEquals(1.0, state.opacity(at(FADE_START)), 0.0);
		assertTrue("the fade should have begun one millisecond later",
			state.opacity(at(FADE_START + 1)) < 1.0);
	}

	/** Kills an opacity that holds at one and drops only at the very end. */
	@Test
	public void opacityKeepsFallingThroughTheFade()
	{
		QuizState state = started();

		double previous = state.opacity(at(FADE_START));
		for (long millis = FADE_START + 25; millis < TOTAL; millis += 25)
		{
			double current = state.opacity(at(millis));
			assertTrue("opacity should have fallen further at " + millis + "ms,"
					+ " was " + previous + " and is " + current,
				current < previous);
			previous = current;
		}
	}

	@Test
	public void opacityIsZeroWhenNothingIsOnScreen()
	{
		QuizState state = started();

		assertEquals(0.0, new QuizState().opacity(START), 0.0001);
		assertEquals(0.0, state.opacity(START.minusHours(1)), 0.0001);
		assertEquals(0.0, state.opacity(at(TOTAL)), 0.0001);
		assertEquals(0.0, state.opacity(at(TOTAL + 5000)), 0.0001);
	}

	@Test
	public void opacityStaysBetweenZeroAndOne()
	{
		QuizState state = started();

		for (long millis = -1000; millis <= TOTAL + 1000; millis += 7)
		{
			double opacity = state.opacity(at(millis));
			assertTrue("opacity too low at " + millis + "ms", opacity >= 0.0);
			assertTrue("opacity too high at " + millis + "ms", opacity <= 1.0);
		}
	}

	// --- nothing ever goes backwards while the box is up ------------------

	@Test
	public void theBoxNeverSlidesBackOutWhileItIsUp()
	{
		QuizState state = started();

		double previous = state.slideProgress(START);
		for (long millis = 5; millis < TOTAL; millis += 5)
		{
			double current = state.slideProgress(at(millis));
			assertTrue("the box slid back at " + millis + "ms", current >= previous);
			previous = current;
		}
	}

	@Test
	public void theBarNeverRefillsAndTheOpacityNeverRises()
	{
		QuizState state = started();

		double previousBar = state.thinkingRemaining(START);
		double previousOpacity = state.opacity(START);
		for (long millis = 5; millis < TOTAL; millis += 5)
		{
			double bar = state.thinkingRemaining(at(millis));
			double opacity = state.opacity(at(millis));
			assertTrue("the bar refilled at " + millis + "ms", bar <= previousBar);
			assertTrue("opacity rose at " + millis + "ms", opacity <= previousOpacity);
			previousBar = bar;
			previousOpacity = opacity;
		}
	}

	// --- the configured durations ----------------------------------------

	@Test
	public void honoursAShorterThinkingTime()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, Duration.ofSeconds(5), ANSWER);

		assertEquals(0.5, state.thinkingRemaining(at(SLIDE_MILLIS + 2500)), 0.0001);
		assertFalse(state.answerVisible(at(SLIDE_MILLIS + 4999)));
		assertTrue(state.answerVisible(at(SLIDE_MILLIS + 5000)));
	}

	@Test
	public void honoursAShorterAnswerTime()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, THINKING, Duration.ofSeconds(3));

		long fadeStart = SLIDE_MILLIS + 10_000 + 3000;
		assertEquals(1.0, state.opacity(at(fadeStart)), 0.0001);
		assertEquals(0.5, state.opacity(at(fadeStart + 300)), 0.0001);
		assertTrue(state.isActive(at(fadeStart + FADE_MILLIS - 1)));
		assertFalse(state.isActive(at(fadeStart + FADE_MILLIS)));
	}

	@Test
	public void honoursALongerThinkingAndAnswerTime()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, Duration.ofSeconds(30), Duration.ofSeconds(45));

		assertFalse(state.answerVisible(at(SLIDE_MILLIS + 29_999)));
		assertTrue(state.answerVisible(at(SLIDE_MILLIS + 30_000)));
		assertTrue(state.isActive(at(SLIDE_MILLIS + 75_000 + FADE_MILLIS - 1)));
		assertFalse(state.isActive(at(SLIDE_MILLIS + 75_000 + FADE_MILLIS)));
	}

	/**
	 * The durations arrive with the start moment and have to be kept with it.
	 * A restart that took the new moment but the old durations would end the
	 * quiz far too early, which is what this pins.
	 */
	@Test
	public void startingAgainReAnchorsTheMomentTheQuestionAndTheDurations()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, Duration.ofSeconds(1), Duration.ofSeconds(1));
		assertTrue("precondition: the first quiz is over by then",
			!state.isActive(at(3000)));

		state.start(at(3000), OTHER_QUESTION, Duration.ofSeconds(20), Duration.ofSeconds(20));

		assertSame(OTHER_QUESTION, state.getQuestion());
		assertEquals(0.0, state.slideProgress(at(3000)), 0.0001);
		assertEquals(1.0, state.thinkingRemaining(at(3000 + SLIDE_MILLIS)), 0.0001);
		assertEquals(0.5, state.thinkingRemaining(at(3000 + SLIDE_MILLIS + 10_000)), 0.0001);
		assertFalse(state.answerVisible(at(3000 + SLIDE_MILLIS + 19_999)));
		assertTrue(state.answerVisible(at(3000 + SLIDE_MILLIS + 20_000)));
		assertEquals(1.0, state.opacity(at(3000 + SLIDE_MILLIS + 40_000)), 0.0001);
		assertTrue(state.isActive(at(3000 + SLIDE_MILLIS + 40_000 + FADE_MILLIS - 1)));
		assertFalse(state.isActive(at(3000 + SLIDE_MILLIS + 40_000 + FADE_MILLIS)));
	}

	@Test
	public void restartingHalfwayThroughAFadeRestoresFullOpacity()
	{
		QuizState state = started();
		assertTrue("half faded before the restart",
			state.opacity(at(FADE_START + 300)) < 0.6);

		state.start(at(FADE_START + 300), OTHER_QUESTION, THINKING, ANSWER);

		assertEquals(1.0, state.opacity(at(FADE_START + 300)), 0.0001);
		assertEquals(0.0, state.slideProgress(at(FADE_START + 300)), 0.0001);
	}

	// --- durations the config could hand over -----------------------------

	@Test
	public void copesWithAThinkingTimeOfZero()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, Duration.ZERO, ANSWER);

		assertEquals(0.0, state.thinkingRemaining(at(SLIDE_MILLIS)), 0.0001);
		assertTrue(state.answerVisible(at(SLIDE_MILLIS)));
		assertTrue(state.isActive(at(SLIDE_MILLIS + 10_000 + FADE_MILLIS - 1)));
		assertFalse(state.isActive(at(SLIDE_MILLIS + 10_000 + FADE_MILLIS)));
	}

	@Test
	public void copesWithAnAnswerTimeOfZero()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, THINKING, Duration.ZERO);

		long fadeStart = SLIDE_MILLIS + 10_000;
		assertEquals(1.0, state.opacity(at(fadeStart)), 0.0001);
		assertEquals(0.5, state.opacity(at(fadeStart + 300)), 0.0001);
		assertFalse(state.isActive(at(fadeStart + FADE_MILLIS)));
	}

	@Test
	public void treatsNegativeDurationsAsZero()
	{
		QuizState state = new QuizState();
		state.start(START, QUESTION, Duration.ofSeconds(-5), Duration.ofSeconds(-5));

		assertTrue(state.isActive(START));
		assertFalse("only the slide and the fade are left",
			state.isActive(at(SLIDE_MILLIS + FADE_MILLIS)));
		for (long millis = 0; millis <= SLIDE_MILLIS + FADE_MILLIS; millis += 3)
		{
			assertTrue(state.slideProgress(at(millis)) >= 0.0);
			assertTrue(state.slideProgress(at(millis)) <= 1.0);
			assertTrue(state.thinkingRemaining(at(millis)) >= 0.0);
			assertTrue(state.thinkingRemaining(at(millis)) <= 1.0);
			assertTrue(state.opacity(at(millis)) >= 0.0);
			assertTrue(state.opacity(at(millis)) <= 1.0);
		}
	}

	@Test
	public void rejectsAStartWithoutAQuestion()
	{
		assertRejected(null, THINKING, ANSWER);
	}

	@Test
	public void rejectsAStartWithoutAThinkingTime()
	{
		assertRejected(QUESTION, null, ANSWER);
	}

	@Test
	public void rejectsAStartWithoutAnAnswerTime()
	{
		assertRejected(QUESTION, THINKING, null);
	}

	private static void assertRejected(QuizQuestion question, Duration thinking, Duration answer)
	{
		QuizState state = new QuizState();
		try
		{
			state.start(START, question, thinking, answer);
			fail("expected the start to be rejected");
		}
		catch (IllegalArgumentException | NullPointerException expected)
		{
			// Everything a start needs comes from the plugin, never from the
			// player, so a missing part is a programming error.
		}
	}
}
