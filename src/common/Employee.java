package common;


public class Employee {
    private String username;
    private String fullName;
    private String Id;
    private String phone;
    private String accountNumber;
    private int branchId;
    private int employeeNumber;
    private Role role;

    public Employee(String username, String fullName, String Id, String phone,
                    String accountNumber, int branchId, int employeeNumber, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.Id = Id;
        this.phone = phone;
        this.accountNumber = accountNumber;
        this.branchId = branchId;
        this.employeeNumber = employeeNumber;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ", " + role + ")";    }
}
