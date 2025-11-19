# Admin Dashboard

This is a SvelteKit application that serves as an admin dashboard for the Android Launcher project. It allows for the management of devices, users, and the viewing of telemetry data.

## Technology Stack

*   **Framework:** [SvelteKit](https://kit.svelte.dev/)
*   **Language:** [TypeScript](https://www.typescriptlang.org/)
*   **Styling:** [Tailwind CSS](https://tailwindcss.com/)
*   **ORM:** [Drizzle ORM](https://orm.drizzle.team/)
*   **Database:** [SQLite](https://www.sqlite.org/index.html)

## Features

*   **Device Management:** Register and manage devices.
*   **User Management:** Manage users of the application.
*   **Telemetry Viewing:** View telemetry data sent from the Android Launcher.

## Project Structure

The project is structured as a typical SvelteKit application:

```
.
├── src
│   ├── lib
│   │   └── server
│   │       └── db       # Drizzle ORM schema and database connection
│   ├── routes
│   │   ├── api          # API endpoints
│   │   ├── devices      # Device management pages
│   │   ├── telemetry    # Telemetry data viewing pages
│   │   └── users        # User management pages
│   └── app.html         # Main HTML template
└── package.json         # Project dependencies and scripts
```

## Development

Once you've created a project and installed dependencies with `npm install` (or `pnpm install` or `yarn`), start a development server:

```sh
npm run dev

# or start the server and open the app in a new browser tab
npm run dev -- --open
```

## Building

To create a production version of your app:

```sh
npm run build
```

You can preview the production build with `npm run preview`.