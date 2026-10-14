package com.laneoptimizer.exception;

/**
 * Thrown when the solver budget is exhausted and feasibility could not be determined.
 */
public class SolverBudgetExceededException extends RuntimeException {

    public SolverBudgetExceededException(String message) {
        super(message);
    }

    public SolverBudgetExceededException() {
        super("Feasibility could not be determined within the search budget. Try reducing the number of transporters or lanes.");
    }
}
