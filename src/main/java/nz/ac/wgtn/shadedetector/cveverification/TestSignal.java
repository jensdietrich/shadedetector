package nz.ac.wgtn.shadedetector.cveverification;

/**
 * Describes the outcome of the test(s) used to demonstrate that a vulnerability is present.
 * If the test is a regression test, this is usually FAIL.
 * @author jens dietrich
 */
public enum TestSignal {
    PASS, FAIL, ERROR
}
