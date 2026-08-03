package com.donator.fun420;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuizDeckTest
{
	private static final QuizQuestion A = new QuizQuestion("A?", "a");
	private static final QuizQuestion B = new QuizQuestion("B?", "b");
	private static final QuizQuestion C = new QuizQuestion("C?", "c");

	private static List<QuizQuestion> deckOf(QuizQuestion... questions)
	{
		return new ArrayList<>(Arrays.asList(questions));
	}

	private static List<QuizQuestion> draw(QuizDeck deck, int count)
	{
		List<QuizQuestion> drawn = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			drawn.add(deck.draw());
		}
		return drawn;
	}

	@Test
	public void dealsEveryQuestionExactlyOnceInAPass()
	{
		List<QuizQuestion> questions = QuizQuestions.ALL;
		QuizDeck deck = new QuizDeck(questions, new Random(1));

		List<QuizQuestion> pass = draw(deck, questions.size());

		assertEquals("a pass should hold as many questions as the deck",
			questions.size(), new HashSet<>(pass).size());
		assertTrue("a pass should hold every question", pass.containsAll(questions));
	}

	@Test
	public void dealsEveryQuestionExactlyOnceInTheSecondPassToo()
	{
		List<QuizQuestion> questions = QuizQuestions.ALL;
		QuizDeck deck = new QuizDeck(questions, new Random(2));
		draw(deck, questions.size());

		List<QuizQuestion> second = draw(deck, questions.size());

		assertEquals(questions.size(), new HashSet<>(second).size());
		assertTrue(second.containsAll(questions));
	}

	@Test
	public void dealsEveryQuestionExactlyOnceInTheFifthPassToo()
	{
		List<QuizQuestion> questions = QuizQuestions.ALL;
		QuizDeck deck = new QuizDeck(questions, new Random(3));
		draw(deck, 4 * questions.size());

		List<QuizQuestion> fifth = draw(deck, questions.size());

		assertEquals(questions.size(), new HashSet<>(fifth).size());
		assertTrue(fifth.containsAll(questions));
	}

	/**
	 * The point of the deck: no repeat is possible until every other question
	 * has been asked. Kills a draw that picks at random each time.
	 */
	@Test
	public void neverRepeatsBeforeThePassIsOver()
	{
		List<QuizQuestion> questions = QuizQuestions.ALL;
		QuizDeck deck = new QuizDeck(questions, new Random(4));

		Set<QuizQuestion> seen = new HashSet<>();
		for (int i = 0; i < questions.size(); i++)
		{
			QuizQuestion drawn = deck.draw();
			assertTrue("repeat within the pass at draw " + i + ": " + drawn,
				seen.add(drawn));
		}
	}

	@Test
	public void shufflesAgainAtThePassBoundary()
	{
		List<QuizQuestion> questions = QuizQuestions.ALL;
		QuizDeck deck = new QuizDeck(questions, new Random(5));

		List<QuizQuestion> first = draw(deck, questions.size());
		List<QuizQuestion> second = draw(deck, questions.size());

		assertNotEquals("the second pass should not repeat the order of the first",
			first, second);
	}

	/**
	 * The one repeat a player would actually notice: the last question of a
	 * pass coming straight back as the first of the next one.
	 */
	@Test
	public void neverRepeatsAcrossThePassBoundary()
	{
		for (long seed = 0; seed < 200; seed++)
		{
			assertNoBackToBackRepeat(deckOf(A, B), seed, 30);
			assertNoBackToBackRepeat(deckOf(A, B, C), seed, 30);
			assertNoBackToBackRepeat(QuizQuestions.ALL, seed, 3 * QuizQuestions.ALL.size());
		}
	}

	private static void assertNoBackToBackRepeat(List<QuizQuestion> questions, long seed, int draws)
	{
		QuizDeck deck = new QuizDeck(questions, new Random(seed));
		List<QuizQuestion> drawn = draw(deck, draws);

		for (int i = 1; i < drawn.size(); i++)
		{
			assertNotEquals("back to back repeat at draw " + i + " with seed " + seed,
				drawn.get(i - 1), drawn.get(i));
		}
	}

	/**
	 * With two questions the no-repeat rule leaves only one possible order,
	 * so the deck has to alternate for ever.
	 */
	@Test
	public void alternatesStrictlyWithTwoQuestions()
	{
		QuizDeck deck = new QuizDeck(deckOf(A, B), new Random(6));

		List<QuizQuestion> drawn = draw(deck, 10);
		QuizQuestion first = drawn.get(0);
		QuizQuestion other = first.equals(A) ? B : A;

		for (int i = 0; i < drawn.size(); i++)
		{
			assertEquals("draw " + i, i % 2 == 0 ? first : other, drawn.get(i));
		}
	}

	/**
	 * A deck of one is the single case where a back to back repeat cannot be
	 * avoided. It keeps handing out that question rather than failing.
	 */
	@Test
	public void keepsDealingTheOnlyQuestionOfASingleQuestionDeck()
	{
		QuizDeck deck = new QuizDeck(deckOf(A), new Random(7));

		for (int i = 0; i < 5; i++)
		{
			assertEquals(A, deck.draw());
		}
	}

	@Test
	public void theSameSeedGivesTheSameOrder()
	{
		QuizDeck one = new QuizDeck(QuizQuestions.ALL, new Random(8));
		QuizDeck other = new QuizDeck(QuizQuestions.ALL, new Random(8));

		assertEquals(draw(one, 60), draw(other, 60));
	}

	/** Kills a deck that ignores the random it was handed. */
	@Test
	public void adifferentSeedGivesADifferentOrder()
	{
		QuizDeck one = new QuizDeck(QuizQuestions.ALL, new Random(9));
		QuizDeck other = new QuizDeck(QuizQuestions.ALL, new Random(10));

		assertNotEquals(draw(one, QuizQuestions.ALL.size()),
			draw(other, QuizQuestions.ALL.size()));
	}

	/** Kills a deck that hands out its questions in the authored order. */
	@Test
	public void doesNotDealInTheAuthoredOrder()
	{
		QuizDeck deck = new QuizDeck(QuizQuestions.ALL, new Random(11));

		assertNotEquals(QuizQuestions.ALL, draw(deck, QuizQuestions.ALL.size()));
	}

	@Test
	public void neverDealsNull()
	{
		QuizDeck deck = new QuizDeck(QuizQuestions.ALL, new Random(12));

		for (int i = 0; i < 60; i++)
		{
			assertNotNull(deck.draw());
		}
	}

	@Test
	public void neverDealsAQuestionThatIsNotInTheDeck()
	{
		QuizDeck deck = new QuizDeck(deckOf(A, B, C), new Random(13));

		for (int i = 0; i < 30; i++)
		{
			QuizQuestion drawn = deck.draw();
			assertTrue("unknown question " + drawn,
				drawn.equals(A) || drawn.equals(B) || drawn.equals(C));
		}
	}

	/** The caller keeps its list; changing it afterwards must not reach the deck. */
	@Test
	public void takesACopyOfTheQuestionList()
	{
		List<QuizQuestion> questions = deckOf(A, B, C);
		QuizDeck deck = new QuizDeck(questions, new Random(14));
		questions.clear();

		Set<QuizQuestion> drawn = new HashSet<>(draw(deck, 3));

		assertEquals(new HashSet<>(Arrays.asList(A, B, C)), drawn);
	}

	@Test
	public void rejectsAnEmptyQuestionList()
	{
		try
		{
			new QuizDeck(new ArrayList<>(), new Random(15));
			fail("expected an empty deck to be rejected");
		}
		catch (IllegalArgumentException expected)
		{
			// A deck with nothing to deal is a programming error.
		}
	}

	@Test
	public void rejectsANullQuestionList()
	{
		try
		{
			new QuizDeck(null, new Random(16));
			fail("expected a null deck to be rejected");
		}
		catch (IllegalArgumentException | NullPointerException expected)
		{
			// As above.
		}
	}

	@Test
	public void rejectsANullRandom()
	{
		try
		{
			new QuizDeck(QuizQuestions.ALL, null);
			fail("expected a null random to be rejected");
		}
		catch (IllegalArgumentException | NullPointerException expected)
		{
			// The random is injected so that tests are deterministic; there
			// is no sensible fallback.
		}
	}
}
