
import { db } from './src/lib/server/db';
import { users } from './src/lib/server/db/schema';
import { randomUUID } from 'crypto';

async function seed() {
    console.log('Seeding database...');

    const password = 'password';
    const passwordHash = Buffer.from(password).toString('base64');

    try {
        await db.insert(users).values({
            id: randomUUID(),
            username: 'admin',
            passwordHash: passwordHash,
            role: 'parent',
            createdAt: new Date()
        });
        console.log('User "admin" created with password "password"');
    } catch (e) {
        console.error('Error seeding database:', e);
    }
}

seed();
