.PHONY: before after

before:
	absc -e HideDelegate-Before.abs DeadlockChecking.abs CogTracking.abs WaitFor.abs

after:
	absc -e HideDelegate-After.abs DeadlockChecking.abs CogTracking.abs WaitFor.abs

test:
	absc -e TopoTest.abs WaitFor.abs
