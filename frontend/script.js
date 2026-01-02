document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const messageDiv = document.getElementById('message');
    
    try {
        const response = await fetch('http://localhost:8080/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        messageDiv.style.display = 'block';
        if (response.ok) {
            messageDiv.className = 'success';
            messageDiv.textContent = 'Login Successful!';
        } else {
            messageDiv.className = 'error';
            messageDiv.textContent = data.message || 'Login Failed!';
        }
    } catch (error) {
        messageDiv.style.display = 'block';
        messageDiv.className = 'error';
        messageDiv.textContent = 'Server Error!';
    }
});
