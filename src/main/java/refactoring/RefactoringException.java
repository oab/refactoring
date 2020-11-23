package refactoring;

public class RefactoringException extends Exception {
    String failure;
    public RefactoringException(String failure) {
        this.failure = failure;
    }

    public String toString() {
        return String.format("RefactoringException: %s",failure);
    }
}
