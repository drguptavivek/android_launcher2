import { json } from '@sveltejs/kit';
import { eq } from 'drizzle-orm';
import { db } from '$lib/server/db';
import { users } from '$lib/server/db/schema';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = async ({ request }) => {
    try {
        const { username, password } = await request.json();

        if (!username || !password) {
            return json({ error: 'Missing credentials' }, { status: 400 });
        }

        // Find user
        const user = await db.query.users.findFirst({
            where: eq(users.username, username)
        });

        if (!user) {
            return json({ error: 'Invalid credentials' }, { status: 401 });
        }

        // Check password (simple hash for now as per user management)
        const passwordHash = Buffer.from(password).toString('base64');

        if (user.passwordHash !== passwordHash) {
            return json({ error: 'Invalid credentials' }, { status: 401 });
        }

        // Return success (and maybe a token in the future)
        return json({
            success: true,
            user: {
                id: user.id,
                username: user.username,
                role: user.role
            }
        });

    } catch (e) {
        console.error('Login error:', e);
        return json({ error: 'Internal server error' }, { status: 500 });
    }
};
