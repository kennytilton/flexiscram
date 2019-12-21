# Flexiscram&trade;
The ultimate Scrabble practice tool.

## In Brief
What we have here are two vanilla projects, one Clojure and one Clojurescript, both created from the vanilla `lein` templates for (a) a Compojure Ring server and (b) a `re-frame` front end. Here is how you can run them.

Begin by cloning this repo, which contains both projects.
````bash
git clone flexiscram.git
````
Now we will run first the service and then the SPA.

### The service
Assuming we are in the root directory holding the two projects:
````bash
cd flexiscram-service
lein deps
lein test # if you like
lein ring server-headless
````
Wait until you see confirmation that it is up and running serving port 3000.

### The FlexiScram&trade; SPA
Back in the root directory of the repo clone:
````bash
cd flexiscram-spa
lein deps
lein figwheel dev
````
When you see that figwheel is waiting for someone to connect, visit `localhost:3449` in a browser.

