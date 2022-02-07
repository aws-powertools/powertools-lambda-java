## Idempotency 
Refer to the [documentation](https://awslabs.github.io/aws-lambda-powertools-java/utilities/idempotency/) for details on how to use this module in your Lambda function.

### Contributing
This module provides a persistence layer with a built-in store using DynamoDB. 
To unit test it, we use [DynamoDB Local](https://docs.aws.amazon.com/fr_fr/amazondynamodb/latest/developerguide/DynamoDBLocal.html) which depends on sqlite.
You may encounter the following issue on Apple M1 chips:
```
com.almworks.sqlite4java.SQLiteException: [-91] cannot load library: java.lang.UnsatisfiedLinkError: native-libs/libsqlite4java-osx-1.0.392.dylib: dlopen(native-libs/libsqlite4java-osx-1.0.392.dylib, 1): no suitable image found.  Did find:
native-libs/libsqlite4java-osx-1.0.392.dylib: no matching architecture in universal wrapper
```

In such case, try with another JDK. See [stackoverflow](https://stackoverflow.com/questions/66635424/dynamodb-local-setup-on-m1-apple-silicon-mac) and this [issue](https://github.com/aws-samples/aws-dynamodb-examples/issues/22) for more info.
We'll update the dependencies as soon as it will be solved.