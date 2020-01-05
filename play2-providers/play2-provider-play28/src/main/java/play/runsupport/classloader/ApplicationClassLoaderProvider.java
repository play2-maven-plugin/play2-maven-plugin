/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport.classloader;

import java.net.URLClassLoader;

public interface ApplicationClassLoaderProvider {
  URLClassLoader get();
}
