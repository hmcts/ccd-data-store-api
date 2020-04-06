# ccd-data-store-api 
[![API v1](https://img.shields.io/badge/API%20Docs-v1-e140ad.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/ccd-data-store-api.v1.json)
[![API v2 (beta)](https://img.shields.io/badge/API%20Docs-v2%20%28beta%29-4286f4.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/ccd-data-store-api.v2.json)
[![Build Status](https://travis-ci.org/hmcts/ccd-data-store-api.svg?branch=master)](https://travis-ci.org/hmcts/ccd-data-store-api)
[![Docker Build Status](https://img.shields.io/docker/build/hmcts/ccd-data-store-api.svg)](https://hub.docker.com/r/hmcts/ccd-data-store-api)
[![codecov](https://codecov.io/gh/hmcts/ccd-data-store-api/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/ccd-data-store-api)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3038977127484764ad0ae9b81a1a14ad)](https://www.codacy.com/app/adr1ancho/ccd-data-store-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hmcts/ccd-data-store-api&amp;utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/hmcts/ccd-data-store-api/badge.svg)](https://snyk.io/test/github/hmcts/ccd-data-store-api)
[![HitCount](http://hits.dwyl.io/SP9gBJ/ccd-data-store-api.svg)](#ccd-data-store-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Store/search cases and provide workbaskets.

### Prerequisites

- [Open JDK 8](https://openjdk.java.net/)
- [Docker](https://www.docker.com)

#### Environment variables
The following environment variables are required:

| Name | Default | Description |
|------|---------|-------------|
| DATA_STORE_DB_HOST | localhost | Host for database |
| DATA_STORE_DB_PORT | 5432 | Port for database |
| DATA_STORE_DB_USERNAME | - | Username for database |
| DATA_STORE_DB_PASSWORD | - | Password for database |
| DATA_STORE_TOKEN_SECRET | `<random string>` | Secret for generating internal JWT tokens for events |
| DATA_STORE_IDAM_KEY | - | Definition store's IDAM S2S micro-service secret key. This must match the IDAM instance it's being run against. |
| DATA_STORE_S2S_AUTHORISED_SERVICES | ccd_gw | Authorised micro-service names for S2S calls |
| IDAM_USER_URL | - | Base URL for IdAM's User API service (idam-app). `http://localhost:4501` for the dockerised local instance or tunneled `dev` instance. |
| IDAM_S2S_URL | - | Base URL for IdAM's S2S API service (service-auth-provider). `http://localhost:4502` for the dockerised local instance or tunneled `dev` instance. |
| USER_PROFILE_HOST | - | Base URL for the User Profile service. `http://localhost:4453` for the dockerised local instance. |
| DEFINITION_STORE_HOST | - | Base URL for the Definition Store service. `http://localhost:4451` for the dockerised local instance. |
| CCD_DM_DOMAIN | - | Base URL for the Document Management domain. |
| AZURE_APPLICATIONINSIGHTS_INSTRUMENTATIONKEY | - | For CNP environment this is provided by the terraform scripts. However any value would do for your local environment. |
| DATA_STORE_DEFAULT_LOG_LEVEL | INFO | Default log level for classes under package uk.gov.hmcts.ccd |
| HTTP_CLIENT_MAX_TOTAL | 100 | Used for Pooling connection manager; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_SECONDS_IDLE_CONNECTION | 120 | Used for Pooling connection manager; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_MAX_CLIENT_PER_ROUTE | 20 | Used for Pooling connection manager; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_VALIDATE_AFTER_INACTIVITY | 0 | Used for Pooling connection manager; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_CONNECTION_TIMEOUT | 30000 | 30 seconds, Used for Pooling connection manager; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_CONNECTION_DRAFTS_CREATE_TIMEOUT | 1000 | 1000 milliseconds, Used for Pooling connection manager for create operation for draft store; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| HTTP_CLIENT_CONNECTION_DRAFTS_TIMEOUT | 500 | 500 milliseconds, Used for Pooling connection manager for draft store; for further information, see https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html |
| DRAFT_STORE_URL | - | Base URL for Draft Store API service. `http://localhost:8800` for the dockerised local instance. |
| DRAFT_ENCRYPTION_KEY | - | Draft encryption key. The encryption key used by draft store to encrypt documents with. |
| DRAFT_TTL_DAYS | - | Number of days after which the saved draft will be deleted if unmodified. |

### Building

The project uses [Gradle](https://gradle.org/).

To build project please execute the following:

```bash
./gradlew clean build
```

### Running

If you want your code to become available to other Docker projects (e.g. for local environment testing), you need to build the image:

```bash
docker-compose build
```

The above will build both the application and database images.  
If you want to build only one of them just specify the name assigned in docker compose file, e.g.:

```bash
docker-compose build ccd-data-store-api
```

When the project has been packaged in `target/` directory, 
you can run it by executing following command:

```bash
docker-compose up
```

As a result the following containers will get created and started:

 - Database exposing port `5452`
 - API exposing ports `4452`

#### Handling database

Database will get initiated when you run `docker-compose up` for the first time by execute all scripts from `database` directory.

You don't need to migrate database manually since migrations are executed every time `docker-compose up` is executed.

You can connect to the database at `http://localhost:5452` with the username and password set in the environment variables.

### Functional Tests
The functional tests are located in `aat` folder. Most of the tests are written using 
befta-fw library, while there are also quite a number of them written using RestAssured. 
The latter group of tests will be decommissioned after the appropriate ones are replaced 
with their new equivalents using befta fw so that the new ones will be 
doing a lot more detailed verifications.  

To find out more about BEFTA Framework, see the repository and its README [here](https://github.com/hmcts/befta-fw).

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
