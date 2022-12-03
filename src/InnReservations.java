import java.sql.*;
import java.util.Scanner;

public class InnReservations {
    void loadDriver() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
//        System.out.println("MySQL JDBC Driver loaded");
    }

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"));
    }

    void createTables(Statement stmt) throws SQLException {
        String sql = "DROP TABLE IF EXISTS lab7_reservations";
        stmt.execute(sql);
        sql = "DROP TABLE IF EXISTS lab7_rooms";
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS lab7_rooms (" +
                "RoomCode char(5) PRIMARY KEY," +
                "RoomName varchar(30) NOT NULL," +
                "Beds int(11) NOT NULL," +
                "bedType varchar(8) NOT NULL," +
                "maxOcc int(11) NOT NULL," +
                "basePrice DECIMAL(6,2) NOT NULL," +
                "decor varchar(20) NOT NULL," +
                "UNIQUE (RoomName)" +
                ")";
        stmt.execute(sql);
        sql = "CREATE TABLE IF NOT EXISTS lab7_reservations (" +
                "CODE int(11) PRIMARY KEY," +
                "Room char(5) NOT NULL," +
                "CheckIn date NOT NULL," +
                "Checkout date NOT NULL," +
                "Rate DECIMAL(6,2) NOT NULL," +
                "LastName varchar(15) NOT NULL," +
                "FirstName varchar(15) NOT NULL," +
                "Adults int(11) NOT NULL," +
                "Kids int(11) NOT NULL," +
                "FOREIGN KEY (Room) REFERENCES lab7_rooms (RoomCode)" +
                ")";
        stmt.execute(sql);
        sql = "INSERT INTO lab7_rooms SELECT * FROM INN.rooms";
        stmt.execute(sql);
        sql = "INSERT INTO lab7_reservations SELECT CODE, Room," +
                "DATE_ADD(CheckIn, INTERVAL 134 MONTH)," +
                "DATE_ADD(Checkout, INTERVAL 134 MONTH)," +
                "Rate, LastName, FirstName, Adults, Kids FROM INN.reservations";
        stmt.execute(sql);
    }

    void runExecution(Statement stmt) throws SQLException {
        String sql = """
                SELECT
                    t1.RoomName, popularity, availability, Days
                FROM
                    (SELECT
                        RoomName, ROUND(SUM(DATEDIFF(CheckOut, CheckIn)) / 180, 2) AS popularity
                    FROM
                        lab7_rooms AS rooms
                    JOIN
                        lab7_reservations AS reservations ON RoomCode = Room
                    GROUP BY
                        RoomName
                    ORDER BY popularity DESC) as t1
                JOIN
                    (SELECT
                        RoomName, MAX(CheckOut) AS availability
                    FROM
                        lab7_rooms AS rooms
                    JOIN
                        lab7_reservations AS reservations ON RoomCode = Room
                    GROUP BY
                        RoomName) as t2 ON t1.RoomName = t2.RoomName
                JOIN
                    (SELECT
                        rooms.RoomName, DATEDIFF(CheckOut, CheckIn) AS Days
                    FROM
                        lab7_rooms AS rooms
                    JOIN
                        lab7_reservations AS reservations ON RoomCode = Room
                    JOIN
                        (SELECT
                            RoomName, MAX(CheckOut) AS FinalDay
                        FROM
                            lab7_rooms AS rooms
                        JOIN
                            lab7_reservations AS reservations ON RoomCode = Room
                        GROUP BY
                            RoomName) AS t1
                    WHERE
                        rooms.RoomName = t1.RoomName AND reservations.CheckOut = t1.FinalDay) AS t3 ON t1.RoomName = t3.RoomName
                ORDER BY
                    popularity DESC;
                """;
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String RoomName = rs.getString("RoomName");
            int popularity = rs.getInt("popularity");
            String availability = rs.getString("availability");
            int Days = rs.getInt("Days");
            System.out.println("Room Name: " + RoomName + ", Popularity: " + popularity + ", Available: " + availability + ", Days: " + Days);
        }
        System.out.println();
    }

    void printOptions() {
        System.out.println("Options: ");
        System.out.println("  1.) Rooms");
        System.out.println("  2.) Exit");
    }

    void run() throws SQLException, ClassNotFoundException {
        this.loadDriver();
        Scanner myObj = new Scanner(System.in);
        String response;
        try (Connection conn = this.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                this.createTables(stmt);

                do {
                    System.out.println("Please enter an option(enter ? to see options): ");
                    response = myObj.nextLine();
                    if(response.equals("1")) {
                        this.runExecution(stmt);
                    }
                    else if(response.equals("?"))
                    {
                        printOptions();
                    }
                }while(!response.equals("2"));

            }
        }
    }

    public static void main(String[] args) throws Exception {
        new InnReservations().run();
    }
}