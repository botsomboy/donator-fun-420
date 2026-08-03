package com.donator.fun420;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuizQuestionTest
{
	@Test
	public void keepsTheQuestionAndTheAnswer()
	{
		QuizQuestion question = new QuizQuestion("What is 420?", "A time and a code.");

		assertEquals("What is 420?", question.getQuestion());
		assertEquals("A time and a code.", question.getAnswer());
	}

	@Test
	public void twoPairsWithTheSameTextAreEqual()
	{
		QuizQuestion one = new QuizQuestion("What is 420?", "A time and a code.");
		QuizQuestion other = new QuizQuestion("What is 420?", "A time and a code.");

		assertEquals(one, other);
		assertEquals(one.hashCode(), other.hashCode());
	}

	@Test
	public void aDifferentQuestionIsNotEqual()
	{
		QuizQuestion one = new QuizQuestion("What is 420?", "A time and a code.");
		QuizQuestion other = new QuizQuestion("What is 421?", "A time and a code.");

		assertNotEquals(one, other);
	}

	@Test
	public void aDifferentAnswerIsNotEqual()
	{
		QuizQuestion one = new QuizQuestion("What is 420?", "A time and a code.");
		QuizQuestion other = new QuizQuestion("What is 420?", "Something else.");

		assertNotEquals(one, other);
	}

	@Test
	public void equalsIsTrueForItselfAndFalseForNullAndOtherTypes()
	{
		QuizQuestion question = new QuizQuestion("What is 420?", "A time and a code.");

		assertTrue(question.equals(question));
		assertFalse(question.equals(null));
		assertFalse(question.equals("What is 420?"));
	}

	/**
	 * The texts that a swapped pair would produce are each other's opposite,
	 * so a constructor that assigned them the wrong way round would survive a
	 * suite that only ever compares whole pairs.
	 */
	@Test
	public void aSwappedPairIsNotEqualToTheOriginal()
	{
		QuizQuestion one = new QuizQuestion("What is 420?", "A time and a code.");
		QuizQuestion swapped = new QuizQuestion("A time and a code.", "What is 420?");

		assertNotEquals(one, swapped);
	}

	@Test
	public void rejectsANullQuestion()
	{
		assertRejected(null, "A time and a code.");
	}

	@Test
	public void rejectsANullAnswer()
	{
		assertRejected("What is 420?", null);
	}

	@Test
	public void rejectsAnEmptyQuestion()
	{
		assertRejected("", "A time and a code.");
	}

	@Test
	public void rejectsAnEmptyAnswer()
	{
		assertRejected("What is 420?", "");
	}

	@Test
	public void rejectsAQuestionOfNothingButWhitespace()
	{
		assertRejected("   ", "A time and a code.");
	}

	@Test
	public void rejectsAnAnswerOfNothingButWhitespace()
	{
		assertRejected("What is 420?", " \t ");
	}

	private static void assertRejected(String question, String answer)
	{
		try
		{
			new QuizQuestion(question, answer);
			fail("expected a rejection of question [" + question + "] and answer [" + answer + "]");
		}
		catch (IllegalArgumentException expected)
		{
			// The authored list holds constants, so a blank entry is a
			// programming error worth failing loudly on.
		}
	}
}
