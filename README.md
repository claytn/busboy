# busboy

> Zero-code REST api generator tool. Clojure implementation of [json-server](https://github.com/typicode/json-server).

This project is in the early stages of development. Basic CRUD operations are working on list data, but it isn't 
very well tested yet and "user-friendliness" hasn't been prioritized.

## Installation

    $ git clone git@github.com:claytn/busboy.git

## Usage

Create an edn file to represent your API data. For example:

```edn
{
  :posts []
  :users []
}
```

This file will act as your local "database" and each key in the outer map will represent a REST endpoint you can hit.
All API operations will read from and make updates to this file directly.

Run busboy with your database file as the input:

    $ clj -m claytn.busboy <filename>

A REST api is now running on localhost:3000. 
You can perform all CRUD operations on the data you just detailed inside your database file.

> Side-note: Unique identifiers are automatically created for new data entries. 
> If you pass an `id` field along with a POST or PUT request it will be ignored.

Examples:

    # Create a post
    $ curl -H 'Content-Type: application/json' -X POST -d '{"title": "First post", "body": "Hello, world!"}' localhost:3000/posts
    
    # Get all posts
    $ curl -X GET localhost:3000/posts
    
    # Get a specific post
    $ curl -X GET localhost:3000/posts/1
    
    # Update a post
    $ curl -H 'Content-Type: application/json' -X PUT -d '{"author": "clayton"}' localhost:3000/posts/1
    
    # Delete a post
    $ curl -X DELETE localhost:3000/posts/1


Run the project's tests (...I haven't written any):

    $ clj -A:test:runner
    
    
## TODOS: 
- Ensure uri paths are validated (currently paths like `//posts///////1` would be accepted due to the way we're splitting the input)
- Singleton data types should be supported in the db file (examples: `{:count 1}`, `{:user {:name "clayton" :age 23}}`)
- Readable/Minimalist logging
- All things CLI (help menu, default inputs, port specification)

## Options

FIXME: listing of options this app accepts.

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2020

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
