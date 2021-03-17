[//]: # " "
[//]: # " Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved. "
[//]: # " "
[//]: # " This program and the accompanying materials are made available under the "
[//]: # " terms of the Eclipse Public License v. 2.0, which is available at "
[//]: # " http://www.eclipse.org/legal/epl-2.0. "
[//]: # " "
[//]: # " This Source Code may also be made available under the following Secondary "
[//]: # " Licenses when the conditions for such availability set forth in the "
[//]: # " Eclipse Public License v. 2.0 are satisfied: GNU General Public License, "
[//]: # " version 2 with the GNU Classpath Exception, which is available at "
[//]: # " https://www.gnu.org/software/classpath/license.html. "
[//]: # " "
[//]: # " SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 "
[//]: # " "

# Welcome to Eclipse Grizzly website repository

## Build

Check out this branch and run ``mvn clean site``

The output will be put into a folder called target/site

To build a site that is viewable on your local system, adjust the properties in src/site/site.xml
to use local resources instead of absolute resources.

Once you are happy, you can check the resulting site files into the gh-pages/docs folder.

## Things to do

+ Someone with suitable maven build skills could automate the site update task. Currently, ``mvn site`` generates "Failed Build" error becasue the github configuration is incorrect.
+ Add back in the javadocs for reference (see src/site/site.xml line 192)
+ Create suitable mechanism for handling 2.x vs. 3.x release docs.
