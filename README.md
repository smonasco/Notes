# Welcome to Notes!
This is a simple RESTful app to store some notes in a Lucene index.
# Easiest Run it
Navigate to build/libs and run the following
>java -jar Notes-all.jar -t

That will allow you to hit all the endpoints, but you will get no persistence as it will use a temporary directory
# OK Gimme Persistence

Navigate to build/libs and run the following
>java -jar Notes-all.jar -d _directory_

It will now create your directory and you can persist data between runs
# Swagger
As a bonus you can get the Swagger UI at
>http://localhost/swager-ui.html

Or the harder to read for humans, swagger documentation (used for various code gen utilities) at
>http://localhost/v2/api-docs

# Endpoints

Curl statements below are made for Windows commandline, but should work for Linux or if not then some small changes to escaping quotes might be needed.
## Post /api/Notes
This will create a note, but shouldn't have an ID.
>curl -i -H "Content-Type: application/json" -X POST -d "{\"body\" : \"Pick up milk!\"}" http://localhost/api/notes
## Get /api/Notes
Lists everything
>curl -i -X GET http://localhost/api/notes
## Get /api/Notes/{id}
Lists the note with the id
>curl -i -X GET http://localhost/api/notes{id}
## Get /api/Notes?query=string
Returns the results of a Lucene search
>curl -i -X GET http://localhost/api/notes?query=milk
## Delete /api/Notes/{id}
Delete your note
>curl -i -XDELETE http://localhost/api/notes/1
## Put /api/Notes/{id}
Updates a note. ID must be present in the URL and if present in the note then it must match. In this case we are reserving the ID generation for the database and indicating that the user may not submit their own.
>curl -i -H "Content-Type: application/json" -XPUT -d "{ \"id\": 3, \"body\" : \"email robert@gmail.com\"}" http://localhost/api/notes/3

# Why Lucene
* It's more performant than SQL queries for `where body like '%milk%'` and the results are probably more user friendly.
* You can do fuzzy searching `mlk~1`
* You can do wildcard matching `mil*`
* You can search for multiple terms `milk AND pick` . Just remember to do URL encoding `milk%20AND%20pick`
* You can search for phrases `"pick up milk"`
* Case is handled
* Basically it's for search
# Unimplemented Interfaces
In an app, you know what's used, and more work than brings value is just that.
Certainly, if it was being used or being presented in a library I would have implemented and tested it.
# Gradle Tasks
Here we use the gradle wrapper. I'm unsure if you will have to download gradle to run them since part of the point of the gradle wrapper is to lock down a version and a jar is used by the wrapper.
## Building
Navigate to the base directory `Notes` and run the following
>./gradlew shadowJar

The output will be `build/libs/Notes-all.jar` a shadow jar is the same concept as an uber jar or a fat jar.
## Testing
Navigate to the base directory `Notes` and run the following
>./gradlew test
