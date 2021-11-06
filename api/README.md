# CACAO_API

This module comprehends the common Java classes and interfaces to be shared with other CACAO modules.

Some common entities are declared at 'org.idb.cacao.api' package and sub-packages.

Each module may declare its own 'repository' classes associated to these common entities for exposing specific methods according to their needs.

This module also declares the 'FileSystemStorageService' component. So other modules interested on accessing files in this common storage should include this package in the Spring configuration regarding component scanning.
