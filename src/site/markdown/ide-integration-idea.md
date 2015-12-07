## IntelliJ IDEA IDE integration

Import steps:

### 1. Import the project as Maven project.

![imp0.png](images/idea/imp0.PNG)

Click **Import Project** button.

![imp.png](images/idea/imp1.PNG)

Select **Maven** external model and click **Next** button.

![imp2.png](images/idea/imp2.PNG)

Accept default values and click **Next** button.

![imp3.png](images/idea/imp3.PNG)

Click **Next** button.

![imp4.png](images/idea/imp4.PNG)

Click **Finish** button.

When import process finishes, the project looks like on the image below.

![imp5.png](images/idea/imp5.PNG)

It requires two additional configuration steps.

### 2. Configure project after import

Open project's popup menu

![imp6.png](images/idea/imp6.PNG)

and click **Maven/Generate Sources and Update Folders** menu.

Maven generates Scala and Java sources from routes file and HTML templates and adds `target/src_managed/main` as additional sources root

![imp7.png](images/idea/imp7.PNG)

### 3. Add Scala support

Open project's popup menu

![imp8.png](images/idea/imp8.PNG)

and click **Add Framework Support...** button.

![imp9.png](images/idea/imp9.PNG)

Select Scala support and click **OK** button.
