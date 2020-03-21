package specification.specification;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import run.run.IsRun;
import run.run.NotRun;
import run.run.ToIsRunGrpc;
import specification.output.IsOutput;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractToIsRunImplBaseImpl extends ToIsRunGrpc.ToIsRunImplBase {

    private final Logger log;
    private boolean initialized = false;

    public AbstractToIsRunImplBaseImpl() {
        super();
        log = Logger.getLogger(getClass().getName());
    }

    @Override
    public void produce(NotRun request, StreamObserver<IsRun> responseObserver) {

        if (initialized) {
            final StatusRuntimeException statusRuntimeException = Status.PERMISSION_DENIED.withDescription("SERVICES CAN ONLY BE INITIALIZED ONCE")
                    .asRuntimeException();
            responseObserver.onError(statusRuntimeException);
            throw statusRuntimeException;
        }


        try {

            startService(request.getIsInput()
                    .getIsSpecificationList());

            responseObserver.onNext(IsRun.newBuilder()
                    .build());

            responseObserver.onCompleted();

            initialized = true;
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            log.log(Level.SEVERE,
                    "Unhandled Error",
                    e);
            System.exit(1);
        }

    }

    private void startService(final List<IsSpecification> isSpecifications) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        ServerBuilder<?> serverBuilder = null;

        for (final IsSpecification isSpecification : isSpecifications) {
            final IsOutput isOutput = isSpecification.getIsOutput();
            final BindableService bindableService = (BindableService) Class.forName(isOutput.getIsServiceNameStringInput() + "ImplBaseImpl")
                    .getConstructor()
                    .newInstance();
            if (isOutput.getIsInProcessServiceBooleanInput()) {
                InProcessServerBuilder.forName(isOutput.getIsServiceNameStringInput())
                        .addService(bindableService)
                        .build()
                        .start();
                System.out.println("Added " + bindableService.toString() + " as in memory");
            } else {
                if (serverBuilder != null) {
                    serverBuilder.addService(bindableService);
                } else {
                    (serverBuilder = ServerBuilder.forPort(8080)).addService(bindableService);
                }
                System.out.println("Added " + bindableService.toString() + " as exposed");
            }
        }

        if (serverBuilder != null) {
            serverBuilder.addService(ProtoReflectionService.newInstance());
            serverBuilder.build()
                    .start();
        }

    }
}
