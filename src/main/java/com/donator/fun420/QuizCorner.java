package com.donator.fun420;

import lombok.AllArgsConstructor;

/**
 * The corner of the screen the quiz box appears in. The corner also settles
 * which side it slides in from: the nearest one.
 */
@AllArgsConstructor
public enum QuizCorner
{
	TOP_LEFT("Top left"),
	TOP_RIGHT("Top right"),
	BOTTOM_LEFT("Bottom left"),
	BOTTOM_RIGHT("Bottom right");

	private final String label;

	/** Whether the box sits against the left edge, and so slides in from it. */
	boolean isLeft()
	{
		return this == TOP_LEFT || this == BOTTOM_LEFT;
	}

	/** Whether the box sits against the top edge. */
	boolean isTop()
	{
		return this == TOP_LEFT || this == TOP_RIGHT;
	}

	/** What the settings panel shows; RuneLite reads the enum through toString. */
	@Override
	public String toString()
	{
		return label;
	}
}
