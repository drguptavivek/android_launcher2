import { defineConfig } from 'drizzle-kit';

export default defineConfig({
	schema: './src/lib/server/db/schema.ts',
	dbCredentials: {
		url: 'sqlite.db'
	},
	dialect: 'sqlite'
});
