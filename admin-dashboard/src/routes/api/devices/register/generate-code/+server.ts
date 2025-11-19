import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { deviceRegistrationTokens } from '$lib/server/db/schema';
import { customAlphabet } from 'nanoid';
import { and, eq, gte, lt } from 'drizzle-orm';

const ALPHANUMERIC = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
const nanoid = customAlphabet(ALPHANUMERIC, 5);

export async function POST({ request }) {
	const { description } = await request.json();

	// Clean up expired tokens
	await db.delete(deviceRegistrationTokens).where(lt(deviceRegistrationTokens.expiresAt, new Date()));

	// Generate a new unique code
	let code;
	let existingToken;
	do {
		code = nanoid();
		existingToken = await db.query.deviceRegistrationTokens.findFirst({
			where: and(
				eq(deviceRegistrationTokens.code, code),
				gte(deviceRegistrationTokens.expiresAt, new Date())
			)
		});
	} while (existingToken);

	const id = customAlphabet(ALPHANUMERIC, 12)();
	const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes from now
	const createdAt = new Date();

	await db.insert(deviceRegistrationTokens).values({
		id,
		code,
		description,
		expiresAt,
		createdAt
	});

	return json({ code, expiresAt }, { status: 201 });
}
