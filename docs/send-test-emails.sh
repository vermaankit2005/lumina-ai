#!/bin/bash
# Send 5 test emails to Mailpit SMTP for local testing

for i in {1..1}; do
  cat > /tmp/email$i.txt << EOF
Subject: Order the dog food
From: sender$i@test.com
To: test@localhost

Hi Darling, Tommy's food is running low. Can you please order more? Thanks!
EOF

  curl --url "smtp://localhost:1025" \
    --mail-from "sender$i@test.com" \
    --mail-rcpt "test@localhost" \
    --upload-file "/tmp/email$i.txt" \
    -s > /dev/null && echo "Email $i sent" || echo "Email $i failed"
done

echo ""
echo "All emails sent to Mailpit on localhost:1025"
echo "Check: curl -s http://localhost:8025/api/v1/messages?limit=10"
