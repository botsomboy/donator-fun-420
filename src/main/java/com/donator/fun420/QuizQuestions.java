package com.donator.fun420;

import java.util.List;

/**
 * The bundled question list. It is a constant in the plugin: there is no file
 * to read and no server to ask, so the quiz works offline and adds nothing to
 * what the plugin needs access to.
 * <p>
 * The tone is factual and neutral throughout: where the term comes from, what
 * the plant is, where the words come from and what the law has done with it.
 * Nothing here advises, encourages or explains how to obtain or use anything.
 * Only well-established facts are stated; a claim that could not be vouched
 * for was left out rather than guessed at.
 * <p>
 * Plain ASCII only, quotation marks included. The game fonts do not render
 * anything else reliably.
 */
public final class QuizQuestions
{
	/** Immutable, and shared by every deck. */
	public static final List<QuizQuestion> ALL = List.of(
		new QuizQuestion(
			"What does \"420\" symbolize in cannabis culture?",
			"A time and code for consumption, originating from high school students."),
		new QuizQuestion(
			"Did \"420\" start as a police radio code for cannabis?",
			"No, that is a myth. It comes from California high school students who met at 4:20 pm in 1971."),
		new QuizQuestion(
			"What did the students who coined \"420\" call themselves?",
			"The Waldos, after the wall outside their school where they used to meet."),
		new QuizQuestion(
			"Which school is tied to the origin of the term \"420\"?",
			"San Rafael High School, in Marin County, California."),
		new QuizQuestion(
			"Which band helped carry the term \"420\" around the world?",
			"The Grateful Dead, whose touring following spread it far beyond California."),
		new QuizQuestion(
			"Which magazine helped popularize \"420\" in the early 1990s?",
			"High Times, which printed a flyer using the term and then took it up itself."),
		new QuizQuestion(
			"Which 1937 United States law first taxed cannabis nationally?",
			"The Marihuana Tax Act, which taxed it out of practical use rather than banning it outright."),
		new QuizQuestion(
			"What did Colorado do with its Mile 420 highway marker?",
			"It put up a sign reading 419.99 instead, because the original kept being stolen."),
		new QuizQuestion(
			"What is the botanical name of the cannabis genus?",
			"Cannabis, a genus in the plant family Cannabaceae."),
		new QuizQuestion(
			"Which brewing plant is in the same family as cannabis?",
			"The hop, Humulus lupulus, also a member of the Cannabaceae."),
		new QuizQuestion(
			"Who gave cannabis its first scientific name?",
			"Carl Linnaeus, who described Cannabis sativa in 1753."),
		new QuizQuestion(
			"Who first described Cannabis indica, and when?",
			"The French naturalist Jean-Baptiste Lamarck, in 1785."),
		new QuizQuestion(
			"What does the species name \"sativa\" mean in Latin?",
			"Roughly \"sown\" or \"cultivated\", a name carried by many crop plants."),
		new QuizQuestion(
			"What is hemp?",
			"Cannabis grown for fibre and seed, carrying only a trace of THC."),
		new QuizQuestion(
			"What was hemp fibre traditionally used for?",
			"Rope, sailcloth, paper and clothing, for thousands of years."),
		new QuizQuestion(
			"What was the 1942 film \"Hemp for Victory\"?",
			"A US government film that asked farmers to grow hemp for the war effort."),
		new QuizQuestion(
			"What does THC stand for?",
			"Tetrahydrocannabinol, the compound most cannabis laws are written around."),
		new QuizQuestion(
			"What does CBD stand for?",
			"Cannabidiol, a compound of the plant that is not intoxicating."),
		new QuizQuestion(
			"Who first worked out the structure of THC?",
			"Raphael Mechoulam and Yechiel Gaoni, in Israel in 1964."),
		new QuizQuestion(
			"What is the body's own compound anandamide named after?",
			"The Sanskrit word \"ananda\", meaning joy or bliss."),
		new QuizQuestion(
			"Where does the English word \"cannabis\" come from?",
			"Latin, which took it from the Greek \"kannabis\", a word of older origin."),
		new QuizQuestion(
			"Where does the word \"marijuana\" come from?",
			"Mexican Spanish, from which American English borrowed it in the late 1800s."),
		new QuizQuestion(
			"What does the word \"hashish\" mean in Arabic?",
			"Dried herb or grass. European languages took the word from Arabic."),
		new QuizQuestion(
			"What is the Single Convention on Narcotic Drugs?",
			"A United Nations treaty from 1961 that shaped cannabis law worldwide."),
		new QuizQuestion(
			"What did the United Nations decide about cannabis in 2020?",
			"It took cannabis out of Schedule IV of the 1961 treaty, the strictest category."),
		new QuizQuestion(
			"Which country legalized cannabis nationwide first?",
			"Uruguay, whose law was passed in December 2013."),
		new QuizQuestion(
			"Which country was the second to legalize cannabis nationwide?",
			"Canada, where the Cannabis Act took effect in October 2018.")
	);

	private QuizQuestions()
	{
	}
}
