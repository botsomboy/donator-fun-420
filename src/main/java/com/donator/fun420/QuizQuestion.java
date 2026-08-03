package com.donator.fun420;

import java.util.Objects;

/**
 * One question with its answer. Immutable, so a deck can hand the same
 * instance to the render thread without either side being able to change it.
 * <p>
 * The texts come from the authored list in {@link QuizQuestions}, never from
 * the player or from a file, so a null or blank text is a programming error
 * and is rejected on the spot rather than drawn as an empty box.
 */
public final class QuizQuestion
{
	private final String question;
	private final String answer;

	public QuizQuestion(String question, String answer)
	{
		this.question = require(question, "question");
		this.answer = require(answer, "answer");
	}

	private static String require(String text, String name)
	{
		if (text == null || text.trim().isEmpty())
		{
			throw new IllegalArgumentException("quiz " + name + " must not be blank");
		}
		return text;
	}

	public String getQuestion()
	{
		return question;
	}

	public String getAnswer()
	{
		return answer;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof QuizQuestion))
		{
			return false;
		}
		QuizQuestion that = (QuizQuestion) other;
		return question.equals(that.question) && answer.equals(that.answer);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(question, answer);
	}

	@Override
	public String toString()
	{
		return "QuizQuestion[" + question + " -> " + answer + "]";
	}
}
