const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: "dalil-mit3mr"
  });
}

async function setAdmin(identifier) {
  if (!identifier) {
    console.error("Usage: node setAdminClaim.js <email-or-uid>");
    process.exit(1);
  }

  try {
    let user;
    if (identifier.includes("@")) {
      user = await admin.auth().getUserByEmail(identifier);
    } else {
      user = await admin.auth().getUser(identifier);
    }

    await admin.auth().setCustomUserClaims(user.uid, {
      admin: true
    });

    console.log(`✅ Successfully granted admin custom claim to ${user.email} (UID: ${user.uid})`);
    console.log("Custom Claims:", { admin: true });
    process.exit(0);
  } catch (error) {
    console.error("❌ Error setting admin claim:", error.message);
    process.exit(1);
  }
}

const target = process.argv[2];
setAdmin(target);
