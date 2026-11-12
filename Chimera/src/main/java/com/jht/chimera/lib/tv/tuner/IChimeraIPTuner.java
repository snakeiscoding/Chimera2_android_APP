package com.jht.chimera.lib.tv.tuner;

import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an IP tuner in the Chimera library, extending the {@link IChimeraTuner} interface with
 * capabilities specific to IP tuners.
 *
 * @param <Channel> The channel type that this tuner works with, extending the {@link IChannel} interface.
 */
public interface IChimeraIPTuner<Channel extends IChannel> extends IChimeraTuner<Channel> {
    /**
     * Asynchronously retrieves the IP address associated with this IP tuner. The method returns
     * a {@link CompletableFuture} that completes with the IP address as a {@link String}. If the
     * IP address cannot be retrieved due to any reason (e.g., network issues, tuner not connected),
     * the future completes exceptionally.
     *
     * @return A {@link CompletableFuture} that, when completed, provides the IP address as a {@link String}.
     */
    CompletableFuture<String> getIpAddress();

    /**
     * Asynchronously retrieves the MAC address of this IP tuner. Similar to {@link #getIpAddress()},
     * this method returns a {@link CompletableFuture} that will complete with the MAC address as a
     * {@link String}. If the tuner is unable to provide the MAC address, the future will complete
     * exceptionally, indicating the failure.
     *
     * @return A {@link CompletableFuture} that, when completed, provides the MAC address as a {@link String}.
     */
    CompletableFuture<String> getMacAddress();
}