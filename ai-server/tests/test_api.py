"""Behavioral API tests, isolated from real user photos and model downloads."""
import io
import os
import sys
import tempfile
from pathlib import Path

os.environ['FACELAB_DATA_ROOT'] = tempfile.mkdtemp(prefix='facelab-test-')
os.environ['FACELAB_API_TOKEN'] = 'test-only-token'
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from fastapi.testclient import TestClient
from PIL import Image
import app as service

client = TestClient(service.app)
AUTH = {'X-FaceLab-Token': 'test-only-token'}


def image_bytes(size=(96, 96)):
    output = io.BytesIO()
    Image.new('RGB', size, (90, 120, 160)).save(output, 'PNG')
    return output.getvalue()


def test_health_and_auth():
    assert client.get('/health').json()['ok'] is True
    assert client.get('/v1/capabilities').status_code == 401
    assert client.get('/v1/capabilities', headers=AUTH).status_code == 200


def test_real_local_enhance_returns_decodable_image():
    response = client.post('/v1/edit', headers=AUTH,
        files={'image': ('test.png', image_bytes(), 'image/png')},
        data={'effect': 'enhance', 'strength': '0.5'})
    assert response.status_code == 200, response.text
    with Image.open(io.BytesIO(response.content)) as image:
        assert image.size == (96, 96)
        assert image.format == 'JPEG'


def test_invalid_image_rejected():
    response = client.post('/v1/edit', headers=AUTH,
        files={'image': ('bad.png', b'not an image', 'image/png')},
        data={'effect': 'enhance'})
    assert response.status_code == 415


def test_small_image_rejected():
    response = client.post('/v1/edit', headers=AUTH,
        files={'image': ('small.png', image_bytes((32, 32)), 'image/png')},
        data={'effect': 'enhance'})
    assert response.status_code == 422


def test_oversized_upload_rejected_before_decode(monkeypatch):
    monkeypatch.setattr(service, 'MAX_BYTES', 32)
    response = client.post('/v1/edit', headers=AUTH,
        files={'image': ('test.png', image_bytes(), 'image/png')},
        data={'effect': 'enhance'})
    assert response.status_code == 413


def test_traversal_rejected():
    assert client.get('/v1/film/jobs/%2e%2e%5csecret', headers=AUTH).status_code == 400


def test_unknown_effect_rejected():
    response = client.post('/v1/edit', headers=AUTH,
        files={'image': ('test.png', image_bytes(), 'image/png')},
        data={'effect': 'nonexistent'})
    assert response.status_code == 400
