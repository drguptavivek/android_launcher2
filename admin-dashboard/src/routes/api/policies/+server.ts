
import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { policies } from '$lib/server/db/schema';
import { randomUUID } from 'crypto';
import { desc } from 'drizzle-orm';

export async function GET() {
    try {
        const allPolicies = await db.select().from(policies).orderBy(desc(policies.createdAt));
        return json(allPolicies);
    } catch (err) {
        console.error(err);
        return json({ error: 'Failed to fetch policies' }, { status: 500 });
    }
}

export async function POST({ request }) {
    try {
        const { name, config } = await request.json();

        if (!name || !config) {
            return json({ error: 'Missing name or config' }, { status: 400 });
        }

        const id = randomUUID();
        const createdAt = new Date();

        await db.insert(policies).values({
            id,
            name,
            config: JSON.stringify(config),
            createdAt
        });

        return json({ success: true, id, name, config, createdAt });
    } catch (err) {
        console.error(err);
        return json({ error: 'Failed to create policy' }, { status: 500 });
    }
}
