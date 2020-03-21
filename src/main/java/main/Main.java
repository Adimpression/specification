package main;

import specification.specification.AbstractToIsRunImplBaseImpl;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.yaml.snakeyaml.Yaml;
import run.input.IsInput;
import run.run.NotRun;
import run.run.ToIsRunGrpc;
import specification.output.IsOutput;
import specification.specification.IsSpecification;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

final public class Main extends AbstractToIsRunImplBaseImpl {


    public static void main(String[] args) throws IOException, InterruptedException {
        final Logger log = Logger.getLogger(Main.class.getName());

        final String appState = System.getenv("app_state");
        if (appState == null || appState.isEmpty()) {
            throw new IllegalStateException("app_state environment variable not set");
        } else {
            System.out.println("app_state=" + appState);
        }

        final String appName = System.getenv("app_name");
        if (appName == null || appName.isEmpty()) {
            throw new IllegalStateException("app_name environment variable not set");
        } else {
            System.out.println("app_name=" + appName);
        }

        final String appDomain = System.getenv("app_domain");
        if (appDomain == null || appDomain.isEmpty()) {
            throw new IllegalStateException("app_domain environment variable not set");
        } else {
            System.out.println("app_domain=" + appDomain);
        }
        final String serviceName = appDomain + "." + (appDomain.contains(".") ? appDomain.substring(appDomain.lastIndexOf('.') + 1) : appDomain) + "." + "To" + appState;

        System.out.println("Inferred entry point to be: " + serviceName);

        final InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(ToIsRunGrpc.SERVICE_NAME);

        inProcessServerBuilder.addService(new Main());

        final Server server = inProcessServerBuilder.directExecutor()
                .build()
                .start();

        final ToIsRunGrpc.ToIsRunBlockingStub client = ToIsRunGrpc.newBlockingStub(InProcessChannelBuilder.forName(ToIsRunGrpc.SERVICE_NAME)
                .build());

        IsInput.Builder builder = IsInput.newBuilder();

        final String filePath = "/etc/dimensions" + "/" + appDomain + ".register.yaml";

        final File file = new File(filePath);

        if (file.exists()) {
            System.out.println("Using .register.yaml: " + filePath);
            final Map<String, Object> yaml = new Yaml().load(new FileInputStream(file));
            final Map<String, Object> artifact = (Map<String, Object>) yaml.get("artifact");

            final Map<String, Object> appSpacialScope = (Map<String, Object>) artifact.get("appSpacialScope");

            final Boolean internet = Objects.requireNonNullElse((Boolean) appSpacialScope.get("internet"),
                    false);
            final Boolean cluster = Objects.requireNonNullElse((Boolean) appSpacialScope.get("cluster"),
                    false);
            final Boolean memory = Objects.requireNonNullElse((Boolean) appSpacialScope.get("memory"),
                    false);


            final List<Map<String, Object>> artifact$dependencies = Objects.requireNonNullElse((List<Map<String, Object>>) artifact.get("dependencies"),
                    new ArrayList<>());

            for (int i = artifact$dependencies.size() - 1; i >= 0; i--) {
                final Map<String, Object> artifact$dependency = artifact$dependencies.get(i);
                final Map<String, Object> artifact$dependency$artifact = (Map<String, Object>) artifact$dependency.get("artifact");
                final String dependencyServiceName;
                {
                    final String dependencyAppName = (String) artifact$dependency$artifact.get("appName");
                    if (dependencyAppName == null || dependencyAppName.isEmpty()) {
                        throw new IllegalStateException("appName dependency not set");
                    } else {
                        System.out.println("appName=" + dependencyAppName);
                    }
                    final String dependencyAppState = (String) artifact$dependency$artifact.get("appState");
                    if (dependencyAppState == null || dependencyAppState.isEmpty()) {
                        throw new IllegalStateException("appState dependency not set");
                    } else {
                        System.out.println("appState=" + dependencyAppState);
                    }
                    final String dependencyAppDomain = (String) artifact$dependency$artifact.get("appDomain");
                    if (dependencyAppDomain == null || dependencyAppDomain.isEmpty()) {
                        throw new IllegalStateException("appDomain dependency not set");
                    } else {
                        System.out.println("appDomain=" + dependencyAppDomain);
                    }
                    dependencyServiceName = dependencyAppDomain + "." + (dependencyAppDomain.contains(".") ? dependencyAppDomain.substring(dependencyAppDomain.lastIndexOf('.') + 1) : dependencyAppDomain) + "." + "To" + dependencyAppState;
                    System.out.println("serviceName:" + dependencyServiceName);
                }

                //Next we add dependent states. This should be made transitive.
                //TODO: Resolve transitive dependencies.
                builder = builder.addIsSpecification(IsSpecification.newBuilder()
                        .setIsOutput(IsOutput.newBuilder()
                                .setIsExposedBooleanInput(false)
                                .setIsInProcessServiceBooleanInput(true)
                                .setIsServiceNameStringInput(dependencyServiceName)
                                .build())
                        .build());
            }

            //TODO: Use appSpacialScope

            //First we add main / entry point state.
            builder = builder.addIsSpecification(IsSpecification.newBuilder()
                    .setIsOutput(IsOutput.newBuilder()
                            .setIsExposedBooleanInput(true)
                            .setIsInProcessServiceBooleanInput(false)
                            .setIsServiceNameStringInput(serviceName)
                            .build())
                    .build());

            //First we add main / entry point state.
            builder = builder.addIsSpecification(IsSpecification.newBuilder()
                    .setIsOutput(IsOutput.newBuilder()
                            .setIsExposedBooleanInput(false)
                            .setIsInProcessServiceBooleanInput(true)
                            .setIsServiceNameStringInput(serviceName)
                            .build())
                    .build());

            final NotRun build = NotRun.newBuilder()
                    .setIsInput(builder)
                    .build();

            build.getIsInput()
                    .getIsSpecificationList()
                    .forEach(isSpecification -> {
                        final String isServiceNameStringInput = isSpecification.getIsOutput()
                                .getIsServiceNameStringInput() + "ImplBaseImpl";
                        try {
                            //Here we try to catch if the dependencies isn't in the classpath so as to prevent client initialization being surprised later.
                            Class.forName(isServiceNameStringInput);
                        } catch (final ClassNotFoundException e) {
                            throw new IllegalStateException("Missing dependency " + isServiceNameStringInput);
                        }
                    });

            client.produce(build);

            server.awaitTermination();
        } else {
            throw new IllegalStateException("Missing required .register.yaml " + filePath);
        }
    }
}
