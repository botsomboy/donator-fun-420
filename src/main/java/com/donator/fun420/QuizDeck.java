package com.donator.fun420;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Deals the questions. A shuffled pass is handed out one by one until it is
 * empty, after which the deck is shuffled again. Picking at random every time
 * would repeat a question within the hour; a pass cannot repeat at all.
 * <p>
 * The {@link Random} is a parameter rather than a field the deck makes for
 * itself, so that a test can pin an order with a fixed seed.
 * <p>
 * Not thread safe: {@link #draw()} is called from the client thread only. The
 * question it returns is immutable and may be handed to the render thread.
 */
public final class QuizDeck
{
	private final List<QuizQuestion> questions;
	private final Random random;

	/** What is left of the current pass, in the order it will be dealt. */
	private final Deque<QuizQuestion> remaining = new ArrayDeque<>();

	/** The last question dealt, so that a new pass cannot open with it. */
	private QuizQuestion lastDrawn;

	public QuizDeck(List<QuizQuestion> questions, Random random)
	{
		if (questions == null || questions.isEmpty())
		{
			throw new IllegalArgumentException("the quiz deck needs at least one question");
		}
		if (random == null)
		{
			throw new IllegalArgumentException("the quiz deck needs a random to shuffle with");
		}

		// A copy, so that the caller's list cannot change what the deck deals.
		this.questions = List.copyOf(questions);
		this.random = random;
	}

	/** The next question. Never null, and never the one just before it. */
	public QuizQuestion draw()
	{
		if (remaining.isEmpty())
		{
			shuffleIntoANewPass();
		}

		lastDrawn = remaining.removeFirst();
		return lastDrawn;
	}

	private void shuffleIntoANewPass()
	{
		List<QuizQuestion> pass = new ArrayList<>(questions);
		Collections.shuffle(pass, random);

		if (pass.size() > 1 && pass.get(0).equals(lastDrawn))
		{
			// The last question of a pass coming straight back as the first of
			// the next is the one repeat a player notices, so the front is
			// swapped with another card. Reshuffling until the front differs
			// would in principle never end; one swap always does.
			Collections.swap(pass, 0, 1 + random.nextInt(pass.size() - 1));
		}

		// A deck of one question is the single case where the repeat cannot be
		// avoided. It keeps dealing that question rather than dealing nothing.
		remaining.addAll(pass);
	}
}
