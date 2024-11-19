# spring-security-demo

## Build and Deploy the Project

```
mvn clean install
```

This is a Spring Boot project, so you can deploy it by simply using the main class: `Application.java`

Once deployed, you can access the app at:

https://localhost:8080

## AuthenticationSuccessHandler configuration for Custom Login Page article

If you want to activate the configuration for the
article [Custom Login Page for Returning User](https://www.chensoul.com/custom-login-page-for-returning-user), then you
need to comment the @Component("myAuthenticationSuccessHandler") annotation in the
MySimpleUrlAuthenticationSuccessHandler and uncomment the same in MyCustomLoginAuthenticationSuccessHandler.

## Feature toggle for Geo IP Lib

The geolocation checks do not work for the IP addresses 127.0.0.1 and 0.0.0.0,
which can be a problem when running the application locally or in a test environment.
To enable/disable the check on the geolocation, set the property `geo.ip.lib.enabled` to true/false; this is false by
default.
