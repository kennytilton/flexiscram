# flexiscram-spa

A [re-frame](https://github.com/Day8/re-frame) application designed to demonstrate re-frame.

## Development Mode

### Run application:
Make sure the FlexiScram service from the sibling directory/project is running, then...

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
