Atlassian Speakeasy is a plugin that allows users to author, enable, and share extensions for Atlassian products.

When getting started developing the Speakeasy plugin, these commands will come in handy

* Start the plugin in the desired product:
  Refapp: mvn refapp:debug
  Confluence: mvn refapp:debug -Dproduct=confluence
  JIRA: mvn refapp:debug -Dproduct=jira

* Deploy the plugin at runtime in the desired product:
  Refapp: mvn refapp:cli
  Confluence: mvn refapp:cli -Dproduct=confluence
  JIRA: mvn refapp:cli -Dproduct=jira

* Deploy the test plugin (code in src/test/resources) with the cli
  'tpi' for test plugin installation, works like the usual 'pi'

* Test your changes in all three products at once - open up three terminals and execute the above refapp:debug commands,
  one per product.  You will also need three more tabs for each of the 'cli' invocations.
