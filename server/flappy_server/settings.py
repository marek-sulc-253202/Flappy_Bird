import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
SECRET_KEY = 'django-insecure-flappy-secret-key'
DEBUG = True
ALLOWED_HOSTS = ['*'] # Důležité pro připojení z mobilu

INSTALLED_APPS = [
    'django.contrib.contenttypes',
    'django.contrib.auth',
    'players',
]

ROOT_URLCONF = 'flappy_server.urls'

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}

USE_TZ = True
