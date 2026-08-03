package com.donator.fun420;

import java.awt.event.MouseEvent;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.MouseAdapter;

/**
 * Catches a left click on the close button of the banner. Every other click is
 * left alone and passes through to the game.
 */
class BannerMouseListener extends MouseAdapter
{
	private final Client client;
	private final Fun420Plugin plugin;
	private final BannerOverlay bannerOverlay;

	@Inject
	BannerMouseListener(Client client, Fun420Plugin plugin, BannerOverlay bannerOverlay)
	{
		this.client = client;
		this.plugin = plugin;
		this.bannerOverlay = bannerOverlay;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}

		// Overlays are only rendered while logged in, so anywhere else the
		// published close button is a leftover from the last drawn frame and
		// would swallow a click over empty screen. Deliberately not the wider
		// rule the plugin uses for the alarm: during LOADING the banner is not
		// drawn either, so the same staleness applies.
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return event;
		}

		if (!plugin.isBannerVisible())
		{
			return event;
		}

		if (bannerOverlay.getCloseButtonBounds().contains(event.getPoint()))
		{
			plugin.dismissBanner();
			event.consume();
		}

		return event;
	}
}
