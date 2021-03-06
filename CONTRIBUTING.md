# Contributing

## Testing your modifications

Use [bower link](http://bower.io/docs/api/#link) to easily test your modifications.

1. Run `bower link` in the folder which contains the bower.json for this component.
2. Run `bower link ist-commun-webui` in the folder which contains the bower.json of the client application.

## Releasing

After thoroughly testing your modifications, follow the steps shown below to release a new version of the component:

1. Change the value of the `version` attribute in `bower.json` to the new semantic version number (see [semver](http://semver.org/)).

2. Commit and push to Github

3. Release version using https://github.com/ist-gr/ist-commun-webui/releases/new
