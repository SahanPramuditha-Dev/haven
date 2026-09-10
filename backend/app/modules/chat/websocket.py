from typing import Dict, List
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
import json
import logging

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Realtime Presence"])

class FamilyConnectionManager:
    def __init__(self):
        # family_id -> list of (user_id, WebSocket)
        self.active_connections: Dict[str, Dict[str, WebSocket]] = {}

    async def connect(self, family_id: str, user_id: str, websocket: WebSocket):
        await websocket.accept()
        if family_id not in self.active_connections:
            self.active_connections[family_id] = {}
        self.active_connections[family_id][user_id] = websocket
        logger.info(f"User {user_id} connected to family {family_id}")
        # Broadcast presence join
        await self.broadcast_presence(family_id, user_id, is_online=True)

    def disconnect(self, family_id: str, user_id: str):
        if family_id in self.active_connections:
            self.active_connections[family_id].pop(user_id, None)
            if not self.active_connections[family_id]:
                del self.active_connections[family_id]
        logger.info(f"User {user_id} disconnected from family {family_id}")

    async def broadcast_presence(self, family_id: str, user_id: str, is_online: bool, status_text: str = None):
        if family_id in self.active_connections:
            payload = {
                "type": "PRESENCE_UPDATE",
                "user_id": user_id,
                "is_online": is_online,
                "status_text": status_text or ("Online" if is_online else "Offline")
            }
            dead_sockets = []
            for uid, ws in self.active_connections[family_id].items():
                try:
                    await ws.send_json(payload)
                except Exception:
                    dead_sockets.append(uid)
            for d in dead_sockets:
                self.disconnect(family_id, d)

    async def broadcast_family_event(self, family_id: str, event_type: str, data: dict):
        if family_id in self.active_connections:
            payload = {
                "type": event_type,
                "data": data
            }
            for uid, ws in list(self.active_connections[family_id].items()):
                try:
                    await ws.send_json(payload)
                except Exception:
                    self.disconnect(family_id, uid)

manager = FamilyConnectionManager()

@router.websocket("/ws/{family_id}/{user_id}")
async def family_websocket_endpoint(websocket: WebSocket, family_id: str, user_id: str):
    await manager.connect(family_id, user_id, websocket)
    try:
        while True:
            raw_data = await websocket.receive_text()
            try:
                msg = json.loads(raw_data)
                action = msg.get("action")
                if action == "UPDATE_STATUS":
                    status_text = msg.get("status_text", "At Home")
                    await manager.broadcast_presence(family_id, user_id, is_online=True, status_text=status_text)
                elif action == "PING":
                    await websocket.send_json({"type": "PONG"})
            except json.JSONDecodeError:
                pass
    except WebSocketDisconnect:
        manager.disconnect(family_id, user_id)
        await manager.broadcast_presence(family_id, user_id, is_online=False)
