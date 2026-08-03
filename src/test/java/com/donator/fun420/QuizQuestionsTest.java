package com.donator.fun420;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QuizQuestionsTest
{
	/** Roughly what the box can hold on one line and in two short sentences. */
	private static final int MAX_QUESTION_LENGTH = 80;
	private static final int MAX_ANSWER_LENGTH = 120;

	@Test
	public void holdsAboutTwentyFivePairs()
	{
		int size = QuizQuestions.ALL.size();

		assertTrue("expected about 25 pairs, found " + size, size >= 24);
		assertTrue("expected about 25 pairs, found " + size, size <= 30);
	}

	@Test
	public void containsThePairSuppliedByTheUser()
	{
		assertTrue(QuizQuestions.ALL.contains(new QuizQuestion(
			"What does \"420\" symbolize in cannabis culture?",
			"A time and code for consumption, originating from high school students.")));
	}

	/**
	 * The police radio code story is the best known claim about the term and
	 * it is wrong, so the deck has to say so, with the facts that replace it.
	 */
	@Test
	public void correctsThePoliceRadioCodeMyth()
	{
		QuizQuestion myth = null;
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			if (question.getQuestion().toLowerCase().contains("police"))
			{
				myth = question;
				break;
			}
		}

		assertNotNull("no pair mentions the police radio code claim", myth);
		String answer = myth.getAnswer();
		assertTrue("the answer should deny the claim: " + answer,
			answer.toLowerCase().contains("no"));
		assertTrue("the answer should name the year 1971: " + answer,
			answer.contains("1971"));
		assertTrue("the answer should name the high school students: " + answer,
			answer.toLowerCase().contains("high school"));
		assertTrue("the answer should name the time 4:20 pm: " + answer,
			answer.contains("4:20 pm"));
	}

	@Test
	public void holdsNoNullEntries()
	{
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertNotNull(question);
		}
	}

	@Test
	public void asksEveryQuestionOnlyOnce()
	{
		Set<String> seen = new HashSet<>();
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertTrue("duplicate question: " + question.getQuestion(),
				seen.add(question.getQuestion()));
		}
	}

	@Test
	public void givesEveryAnswerOnlyOnce()
	{
		Set<String> seen = new HashSet<>();
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertTrue("duplicate answer: " + question.getAnswer(),
				seen.add(question.getAnswer()));
		}
	}

	@Test
	public void cannotBeAddedTo()
	{
		try
		{
			QuizQuestions.ALL.add(new QuizQuestion("What is 420?", "A time and a code."));
			fail("the bundled deck should not be modifiable");
		}
		catch (UnsupportedOperationException expected)
		{
			// The list is shared by every deck, so nobody may change it.
		}
	}

	@Test
	public void cannotBeClearedOrRemovedFrom()
	{
		try
		{
			QuizQuestions.ALL.remove(0);
			fail("the bundled deck should not be modifiable");
		}
		catch (UnsupportedOperationException expected)
		{
			// As above.
		}
	}

	/**
	 * The game fonts do not render anything outside plain ASCII reliably, and
	 * a typographic quotation mark pasted in from a document is the easiest
	 * way for one to sneak in.
	 */
	@Test
	public void usesPlainAsciiOnly()
	{
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertAscii("question", question.getQuestion());
			assertAscii("answer", question.getAnswer());
		}
	}

	private static void assertAscii(String what, String text)
	{
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c < 0x20 || c > 0x7E)
			{
				fail("non-ascii character 0x" + Integer.toHexString(c)
					+ " at index " + i + " in " + what + ": " + text);
			}
		}
	}

	@Test
	public void keepsEveryPairShortEnoughForTheBox()
	{
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertTrue("question too long (" + question.getQuestion().length() + "): "
					+ question.getQuestion(),
				question.getQuestion().length() <= MAX_QUESTION_LENGTH);
			assertTrue("answer too long (" + question.getAnswer().length() + "): "
					+ question.getAnswer(),
				question.getAnswer().length() <= MAX_ANSWER_LENGTH);
		}
	}

	@Test
	public void asksSomethingInEveryQuestion()
	{
		for (QuizQuestion question : QuizQuestions.ALL)
		{
			assertTrue("not phrased as a question: " + question.getQuestion(),
				question.getQuestion().endsWith("?"));
		}
	}

	@Test
	public void isTheSameListOnEveryRead()
	{
		List<QuizQuestion> first = QuizQuestions.ALL;
		List<QuizQuestion> second = QuizQuestions.ALL;

		assertEquals(first, second);
	}
}
