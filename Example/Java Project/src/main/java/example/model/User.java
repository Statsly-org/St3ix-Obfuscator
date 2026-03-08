package example.model;

/**
 * Simple model class.
 */
public final class User {

    private final String name;
    private final int id;

    public User(String name, int id) {
        this.name = name;
        this.id = id;
        validateId(id);
    }

    private static void validateId(int id) {
        if (id < 0 || id > 999999) {
            throw new IllegalArgumentException("ID out of range");
        }
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', id=" + id + "}";
    }
}
