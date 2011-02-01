package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads.  Patterned off the
 * {@link org.osgi.util.tracker.ServiceTracker}.
 *
 * @since 2.6.0
 */
public interface PluginModuleTracker<M, T extends ModuleDescriptor<M>>
{
    /**
     * Implement this to customize how and which descriptors are stored
     */
    interface Customizer<M, T extends ModuleDescriptor<M>>
    {
        /**
         * Called before adding the descriptor to the internal tracker
         * @param descriptor The new descriptor
         * @return The descriptor to track
         */
        T adding(T descriptor);

        /**
         * Called after the descriptor has been removed from the internal tracker
         * @param descriptor The descriptor that was removed
         */
        void removed(T descriptor);

    }

    /**
     * @return a snapshot of the currently tracked enabled module descriptors
     */
    Iterable<T> getModuleDescriptors();

    /**
     * Gets a snapshot of the currently tracked enabled module instances
     * @return The module instances
     */
    Iterable<M> getModules();

    /**
     * @return The number of module descriptors currently tracked.  Should only be used for reporting purposes as it
     * only reflects the size at exactly the calling time.
     */
    int size();

    /**
     * Closes the tracker.  Ensure you call this, or you may cause a memory leak.
     */
    void close();
}
