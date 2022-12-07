import java.sql.*;
import java.util.ArrayList;
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

    void Rooms(Statement stmt) throws SQLException {
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
    void Reservations(Statement stmt) throws SQLException {
        Scanner myObj = new Scanner(System.in);
        String firstName;
        String lastName;
        String date1;
        String date2 = null;
        String roomCode;
        String numAdults;
        String numChildren;
        String bedType;

        System.out.println("Please Enter a first name: ");
        firstName = myObj.nextLine();
        System.out.println("Please Enter a last name: ");
        lastName = myObj.nextLine();
        System.out.println("Please Enter a first date(YYYY-MM-DD): ");
        date1 = myObj.nextLine();
        if(!date1.isEmpty()) {
            System.out.println("Please Enter a second date(YYYY-MM-DD): ");
            date2 = myObj.nextLine();
        }
        System.out.println("Please Enter a room code: ");
        roomCode = myObj.nextLine();
        System.out.println("Please Enter a number of children: ");
        numChildren = myObj.nextLine();
        System.out.println("Please Enter a number of adults: ");
        numAdults = myObj.nextLine();
        System.out.println("Please Enter a bed type: ");
        bedType = myObj.nextLine();

        String sql = """
                SELECT
                    RoomName, basePrice
                FROM
                    lab7_reservations as reservations
                JOIN
                    lab7_rooms as rooms ON RoomCode = Room
                """;
        int moreThanOne = 0;
        if(!date1.isEmpty() && !date2.isEmpty()) {
            sql += """
                    WHERE
                        !('%s' BETWEEN CheckIn AND CheckOut
                        OR
                        '%s' BETWEEN CheckIn AND CheckOut)
                    """.formatted(date1, date2);
            moreThanOne = 1;
        }

        if(!roomCode.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
            else {
                sql += """
                WHERE roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!numChildren.isEmpty() || !numAdults.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND %s + %s <= maxOcc
                """.formatted(numChildren, numAdults);
            }
            else {
                sql += """
                WHERE %s + %s <= maxOcc
                """.formatted(numChildren, numAdults);
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!bedType.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND bedType = '%s'
                """.formatted(bedType.toUpperCase());
            }
            else {
                sql += """
                WHERE bedType = '%s'
                """.formatted(bedType.toUpperCase());
            }
        }
        sql += """
                GROUP BY
                    RoomName, basePrice
                """;
        ResultSet rs = stmt.executeQuery(sql);
        ArrayList<String> rooms = new ArrayList<>();
        System.out.println("Rooms available: ");
        while (rs.next()) {
            String RoomName = rs.getString("RoomName");
            int basePrice = rs.getInt("basePrice");
            System.out.println(rooms.size() + 1 + ".) Room Name: " + RoomName + ", Base Price: " + basePrice);
            rooms.add(RoomName);
        }
        System.out.println();
    }
    void ReservationChange(Statement stmt) throws SQLException {
        Scanner myObj = new Scanner(System.in);
        String reservationCode;
        String firstName;
        String lastName;
        String date1;
        String date2 = null;
        String roomCode;

        System.out.println("Please Enter a reservation code for reservation modification: ");
        reservationCode = myObj.nextLine();
        if(reservationCode.isEmpty())
        {
            return;
        }
        System.out.println("Please Enter a first name(if change): ");
        firstName = myObj.nextLine();
        System.out.println("Please Enter a last name(if change): ");
        lastName = myObj.nextLine();
        System.out.println("Please Enter a first date(YYYY-MM-DD)(if change): ");
        date1 = myObj.nextLine();
        if(!date1.isEmpty()) {
            System.out.println("Please Enter a second date(YYYY-MM-DD): ");
            date2 = myObj.nextLine();
        }
        System.out.println("Please Enter a room code(if change): ");
        roomCode = myObj.nextLine();

        String sql = """
                UPDATE
                    lab7_reservations
                """;
        int moreThanOne = 0;
        if(!firstName.isEmpty()) {
            sql += """
                SET
                    FirstName = '%s'
                """.formatted(firstName.toUpperCase());
            moreThanOne = 1;
        }

        if(!lastName.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                LastName = '%s'
                """.formatted(lastName.toUpperCase());
            }
            else {
                sql += """
                SET
                    LastName = '%s'
                """.formatted(lastName.toUpperCase());
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!date1.isEmpty() && !date2.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                '%s' = CheckIn AND '%s' = CheckOut
                """.formatted(date1, date2);
            }
            else {
                sql += """
                SET
                    CheckIn = %s
                    CheckOut = %s
                """.formatted(date1, date2);
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!roomCode.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
            else {
                sql += """
                SET
                    roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
        }
        sql += """
                WHERE
                    CODE = %s
                """.formatted(reservationCode);
        stmt.executeUpdate(sql);
    }
    void ReservationCancellation(Statement stmt) throws SQLException {
        Scanner myObj = new Scanner(System.in);
        String reservationCode;
        System.out.println("Please Enter a reservation code for cancellation: ");
        reservationCode = myObj.nextLine();
        if(reservationCode.isEmpty()) {
            return;
        }
        String sql = """
                DELETE FROM lab7_reservations
                    WHERE CODE = %s;
                """.formatted(reservationCode);
        stmt.executeUpdate(sql);
        System.out.println();
    }
    void DetailedReservationInformation(Statement stmt) throws SQLException {
        Scanner myObj = new Scanner(System.in);
        String firstName;
        String lastName;
        String date1;
        String date2 = null;
        String roomCode;
        String reservationCode;

        System.out.println("Please Enter a first name: ");
        firstName = myObj.nextLine();
        System.out.println("Please Enter a last name: ");
        lastName = myObj.nextLine();
        System.out.println("Please Enter a first date(YYYY-MM-DD): ");
        date1 = myObj.nextLine();
        if(!date1.isEmpty()) {
            System.out.println("Please Enter a second date(YYYY-MM-DD): ");
            date2 = myObj.nextLine();
        }
        System.out.println("Please Enter a room code: ");
        roomCode = myObj.nextLine();
        System.out.println("Please Enter a reservation code: ");
        reservationCode = myObj.nextLine();

        String sql = """
                SELECT
                    FirstName, LastName, CheckIn, CheckOut, RoomCode, CODE
                FROM
                    lab7_reservations as reservations
                JOIN
                    lab7_rooms as rooms ON RoomCode = Room
                """;
        int moreThanOne = 0;
        if(!firstName.isEmpty()) {
            sql += """
                WHERE FirstName = '%s'
                """.formatted(firstName.toUpperCase());
            moreThanOne = 1;
        }

        if(!lastName.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND LastName = '%s'
                """.formatted(lastName.toUpperCase());
            }
            else {
                sql += """
                WHERE LastName = '%s'
                """.formatted(lastName.toUpperCase());
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!date1.isEmpty() && !date2.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND '%s' = CheckIn AND '%s' = CheckOut
                """.formatted(date1, date2);
            }
            else {
                sql += """
                WHERE '%s' = CheckIn AND '%s' = CheckOut
                """.formatted(date1, date2);
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!roomCode.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
            else {
                sql += """
                WHERE roomCode = '%s'
                """.formatted(roomCode.toUpperCase());
            }
            if(moreThanOne == 0)
            {
                moreThanOne = 1;
            }
        }

        if(!reservationCode.isEmpty()) {
            if(moreThanOne == 1)
            {
                sql += """
                AND CODE = %s
                """.formatted(reservationCode);
            }
            else {
                sql += """
                WHERE CODE = %s
                """.formatted(reservationCode);
            }
        }

        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String FirstName = rs.getString("FirstName");
            String LastName = rs.getString("LastName");
            String CheckIn = rs.getString("CheckIn");
            String CheckOut = rs.getString("CheckOut");
            String RoomCode = rs.getString("RoomCode");
            int CODE = rs.getInt("CODE");
            System.out.println("First Name: " + FirstName + ", Last Name: " + LastName + ", Check In: " + CheckIn + ", Check Out: " + CheckOut +
                    ", Room Code: " + RoomCode + ", Code: " + CODE);
        }
        System.out.println();
    }
    void Revenue(Statement stmt) throws SQLException {
        String sql = """
                SELECT
                    RoomName, MONTH(CheckOut) AS Month, ROUND(SUM(DATEDIFF(CheckOut, CheckIn) * Rate), 2) AS PAID
                FROM
                    lab7_reservations as reservations
                JOIN
                    lab7_rooms as rooms ON RoomCode = Room
                WHERE
                    YEAR(CheckIn) = YEAR(CURRENT_DATE)
                    AND
                    YEAR(CheckOut) = YEAR(CURRENT_DATE)
                GROUP BY
                    RoomName, MONTH(CheckOut)
                ORDER BY
                     RoomName, Month;
                """;
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String roomName = rs.getString("RoomName");
            int month = rs.getInt("Month");
            double paid = rs.getDouble("PAID");
            String result = "Room Name: " + roomName;
            if(month == 1) {
                result += ", Month: January, Total: $" + paid;
            }
            else if(month == 2) {
                result += ", Month: February, Total: $" + paid;
            }
            else if(month == 3) {
                result += ", Month: March, Total: $" + paid;
            }
            else if(month == 4) {
                result += ", Month: April, Total: $" + paid;
            }
            else if(month == 5) {
                result += ", Month: May, Total: $" + paid;
            }
            else if(month == 6) {
                result += ", Month: June, Total: $" + paid;
            }
            else if(month == 7) {
                result += ", Month: July, Total: $" + paid;
            }
            else if(month == 8) {
                result += ", Month: August, Total: $" + paid;
            }
            else if(month == 9) {
                result += ", Month: September, Total: $" + paid;
            }
            else if(month == 10) {
                result += ", Month: October, Total: $" + paid;
            }
            else if(month == 11) {
                result += ", Month: November, Total: $" + paid;
            }
            else if(month == 12) {
                result += ", Month: December, Total: $" + paid;
            }
            System.out.println(result);
        }
        System.out.println();
        sql = """
                    SELECT
                        RoomName, ROUND(SUM(DATEDIFF(CheckOut, CheckIn) * Rate), 2) AS PAID
                    FROM
                        lab7_reservations as reservations
                    JOIN
                        lab7_rooms as rooms ON RoomCode = Room
                    WHERE
                        YEAR(CheckIn) = YEAR(CURRENT_DATE)
                        AND
                        YEAR(CheckOut) = YEAR(CURRENT_DATE)
                    GROUP BY
                        RoomName
                    ORDER BY
                         RoomName;
                """;
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String roomName = rs.getString("RoomName");
            double paid = rs.getDouble("PAID");

            System.out.println("Room Name: " + roomName + ", Total: $" + paid);
        }
        System.out.println();
    }

    void printOptions() {
        System.out.println("Options: ");
        System.out.println("  1.) Rooms");
        System.out.println("  2.) Reservations");
        System.out.println("  3.) Reservation Change");
        System.out.println("  4.) Reservation Cancellation");
        System.out.println("  5.) Detailed Reservation Information");
        System.out.println("  6.) Revenue");
        System.out.println("  7.) Exit");
        System.out.println();
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
                    switch (response) {
                        case "1" -> this.Rooms(stmt);
                        case "2" -> this.Reservations(stmt);
                        case "3" -> this.ReservationChange(stmt);
                        case "4" -> this.ReservationCancellation(stmt);
                        case "5" -> this.DetailedReservationInformation(stmt);
                        case "6" -> this.Revenue(stmt);
                        case "?" -> printOptions();
                    }
                }while(!response.equals("7"));

            }
        }
    }

    public static void main(String[] args) throws Exception {
        new InnReservations().run();
    }
}