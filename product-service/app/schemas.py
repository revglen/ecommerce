from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field, validator

class ProductBase(BaseModel):
    name:str=Field(..., max_length=255)
    description:Optional[str]=Field(None, max_length=1000)
    price:float = Field(..., gt=0)
    category:Optional[str]=Field(None, max_length=100)
    stock:int=Field(0, ge=0)

class ProductCreate(ProductBase):
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

class ProductUpdate(BaseModel):
    name: Optional[str]=Field(None, max_length=255)
    description:Optional[str]=Field(None, max_length=1000)
    price:float = Field(..., gt=0)
    category:Optional[str]=Field(None, max_length=100)
    stock:int=Field(0, ge=0)
    is_active:Optional[bool]=None

class Product(ProductBase):
    id:int
    is_active:bool
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        orm_mode=True

class ProductResponse(BaseModel):
    success: bool = True
    data: Product

class ProductListResponse(BaseModel):
    success: bool = True
    data: list[Product]
    count: int
