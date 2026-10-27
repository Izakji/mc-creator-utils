package com.izakji.mccreatorutils.locate.engine;

/**
 * Work submitted to the locate service.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface LocateTask {

    LocateResult run(CancellationToken token) throws Exception;
}
