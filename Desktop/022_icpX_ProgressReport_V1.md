# Project Progress Report

**1. Project Title**
icpX

**2. Short description of the project**
icpX is a desktop application designed to assist competitive programmers in tracking their progress and managing training goals. Built with JavaFX and SQLite, it provides a centralized dashboard that integrates with the Codeforces API to fetch user statistics, visualize performance data, and manage custom training targets.

**3. Brief summary of work completed so far**

I have successfully established the core foundation of the application and implemented several key features:

*   **Project Architecture**: Set up the project using Maven and the Model-View-Controller (MVC) design pattern to ensure code maintainability and scalability.
*   **Database Integration**: Integrated SQLite for local data storage and implemented Data Access Objects (DAOs) for managing User, Target, and Settings data.
*   **Authentication System**: Developed a secure login and setup mechanism. This includes password hashing using BCrypt and a "first-run" setup wizard for new users.
*   **User Interface**: Designed and implemented a modern, responsive dashboard using JavaFX and CSS. The UI includes a sidebar navigation and dynamic content switching.
*   **Target Management**: Implemented the functionality for users to create, view, and delete training targets (e.g., "Solve 50 problems").
*   **API Integration**: Created the `CodeforcesService` to connect with the Codeforces API, allowing the application to retrieve contest and problem data.

**Relevant Screenshots**

[Screenshot 1: Login and Setup View]

[Screenshot 2: Main Dashboard with Statistics]

[Screenshot 3: Target Management Interface]

**4. Screenshot from my GitHub repository**

[Screenshot: GitHub Insights -> Contributors -> Commits Over Time]

**5. Suggestions addressed**

*   **Suggestion**: N/A
    *   **How addressed**: N/A

**6. Suggestions planned to be addressed**

*   N/A

**7. My next plan of action**

My primary focus for the next phase of development includes:

*   **Enhanced Data Visualization**: Implementing interactive charts (line and bar charts) to visually track rating changes and problem-solving rates over time.
*   **Advanced API Features**: Expanding the Codeforces integration to fetch detailed submission history and analyze weak topic areas.
*   **Problem Recommendation Engine**: Developing an algorithm to suggest practice problems based on the user's current skill level and defined targets.
*   **Virtual Contests**: Adding a feature to simulate past contests with a timer and scoreboard to mimic real competition environments.
*   **Testing and Optimization**: Conducting rigorous unit and UI testing to ensure application stability and performance.
