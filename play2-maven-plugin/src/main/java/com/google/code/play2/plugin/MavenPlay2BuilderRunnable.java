package com.google.code.play2.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;

public class MavenPlay2BuilderRunnable implements Runnable
{
    private LifecycleExecutor lifecycleExecutor;

    private MavenSession session;

    public MavenPlay2BuilderRunnable( LifecycleExecutor lifecycleExecutor, MavenSession session )
    {
        this.lifecycleExecutor = lifecycleExecutor;
        this.session = session;
    }

    @Override
    public void run()
    {
        lifecycleExecutor.execute( session );
    }
}