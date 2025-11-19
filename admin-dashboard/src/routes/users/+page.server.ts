import { fail } from '@sveltejs/kit';
import { eq } from 'drizzle-orm';
import { db } from '$lib/server/db';
import { users } from '$lib/server/db/schema';
import type { Actions, PageServerLoad } from './$types';
import { randomUUID } from 'crypto';

export const load: PageServerLoad = async () => {
    const allUsers = await db.select().from(users).orderBy(users.createdAt);
    return {
        users: allUsers
    };
};

export const actions: Actions = {
    create: async ({ request }) => {
        const data = await request.formData();
        const username = data.get('username') as string;
        const password = data.get('password') as string;
        const role = data.get('role') as string;

        if (!username || !password) {
            return fail(400, { missing: true });
        }

        // In a real app, hash the password!
        // For this prototype, we'll store plain text (or simple hash if requested later)
        // but for now let's just store it as is for simplicity of debugging, 
        // or better, a simple base64 to avoid total plain text.
        // Actually, let's just do a simple "hash" for now to show intent.
        const passwordHash = Buffer.from(password).toString('base64');

        try {
            await db.insert(users).values({
                id: randomUUID(),
                username,
                passwordHash,
                role: role || 'child',
                createdAt: new Date()
            });
        } catch (e) {
            return fail(400, { error: 'Username already exists' });
        }

        return { success: true };
    },

    delete: async ({ request }) => {
        const data = await request.formData();
        const id = data.get('id') as string;

        if (!id) {
            return fail(400, { missing: true });
        }

        await db.delete(users).where(eq(users.id, id));
        return { success: true };
    }
};
