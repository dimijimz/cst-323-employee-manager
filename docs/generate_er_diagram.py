# -*- coding: utf-8 -*-
"""ER diagram for the Employee Manager schema, built from V1__create_schema.sql.

Laid out in typographic points (1 data unit == 1 pt) with box widths measured from
the monospace advance width, so nothing can overflow its box. Greyscale only, white
ground: prints cleanly and survives a black-and-white printer.
"""
import sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, Circle

INK, RULE, TYPE_INK, HEAD_FILL, SUBTLE, HAIR = (
    "#1a1a1a", "#3a3a3a", "#4a4a4a", "#e7e7e7", "#666666", "#c8c8c8")
SANS, MONO = "DejaVu Sans", "DejaVu Sans Mono"
ADV = 0.602  # DejaVu Sans Mono advance width, in em

F_TITLE, F_SUB, F_HEAD, F_NAME, F_TYPE, F_BADGE, F_NOTE = 15, 9.5, 13, 10, 8.4, 7.6, 8.6

DEPARTMENT = [
    ("PK", "id",   "BIGINT NOT NULL AUTO_INCREMENT"),
    ("",   "name", "VARCHAR(100) NOT NULL UNIQUE"),
]
EMPLOYEE = [
    ("PK", "id",            "BIGINT NOT NULL AUTO_INCREMENT"),
    ("",   "first_name",    "VARCHAR(50) NOT NULL"),
    ("",   "last_name",     "VARCHAR(50) NOT NULL"),
    ("",   "email",         "VARCHAR(120) NOT NULL UNIQUE"),
    ("",   "hire_date",     "DATE NOT NULL"),
    ("FK", "department_id", "BIGINT NOT NULL"),
]

PAD, GUTTER, COLGAP = 9.0, 22.0, 12.0
ROW_H, HEAD_H = 19.0, 26.0

def mono_w(text, size):
    return len(text) * ADV * size

def box_width(rows):
    name_w = max(mono_w(r[1], F_NAME) for r in rows)
    type_w = max(mono_w(r[2], F_TYPE) for r in rows)
    return PAD + GUTTER + name_w + COLGAP + type_w + PAD, name_w

def draw(ax, x, rows, title, width, name_w):
    height = HEAD_H + ROW_H * len(rows)
    top = height / 2.0
    ax.add_patch(Rectangle((x, -height / 2.0), width, height, facecolor="white",
                           edgecolor=RULE, linewidth=1.6, zorder=2))
    ax.add_patch(Rectangle((x, top - HEAD_H), width, HEAD_H, facecolor=HEAD_FILL,
                           edgecolor=RULE, linewidth=1.6, zorder=3))
    ax.text(x + width / 2.0, top - HEAD_H / 2.0, title, ha="center", va="center",
            fontsize=F_HEAD, fontweight="bold", family=MONO, color=INK, zorder=4)
    name_x = x + PAD + GUTTER
    type_x = name_x + name_w + COLGAP
    y = top - HEAD_H
    for i, (badge, col, typ) in enumerate(rows):
        y -= ROW_H
        cy = y + ROW_H / 2.0
        if badge:
            ax.text(x + PAD, cy, badge, ha="left", va="center", fontsize=F_BADGE,
                    fontweight="bold", family=MONO, color=INK, zorder=4)
        ax.text(name_x, cy, col, ha="left", va="center", fontsize=F_NAME,
                family=MONO, color=INK, zorder=4)
        ax.text(type_x, cy, typ, ha="left", va="center", fontsize=F_TYPE,
                family=MONO, color=TYPE_INK, zorder=4)
        if i < len(rows) - 1:
            ax.plot([x + PAD * 0.6, x + width - PAD * 0.6], [y, y],
                    color=HAIR, linewidth=0.8, zorder=3)
    return height

dep_w, dep_name_w = box_width(DEPARTMENT)
emp_w, emp_name_w = box_width(EMPLOYEE)
GAP = 96.0
dep_x = 0.0
emp_x = dep_x + dep_w + GAP
content_w = emp_x + emp_w

dep_h = HEAD_H + ROW_H * len(DEPARTMENT)
emp_h = HEAD_H + ROW_H * len(EMPLOYEE)
half = max(dep_h, emp_h) / 2.0

TITLE_Y, SUB_Y = half + 52, half + 30
FK_Y, NOTE1_Y, NOTE2_Y = -half - 26, -half - 48, -half - 66
MARGIN = 14.0
x0, x1 = -MARGIN, content_w + MARGIN
y0, y1 = NOTE2_Y - MARGIN, TITLE_Y + MARGIN

fig, ax = plt.subplots(figsize=((x1 - x0) / 72.0, (y1 - y0) / 72.0))
fig.patch.set_facecolor("white")
ax.set_facecolor("white")
ax.set_xlim(x0, x1); ax.set_ylim(y0, y1)
ax.set_position([0, 0, 1, 1]); ax.axis("off")

draw(ax, dep_x, DEPARTMENT, "department", dep_w, dep_name_w)
draw(ax, emp_x, EMPLOYEE, "employee", emp_w, emp_name_w)

# --- relationship, crow's foot ----------------------------------------------
LX, RX = dep_x + dep_w, emp_x
ax.plot([LX, RX], [0, 0], color=RULE, linewidth=1.6, zorder=1)

# department end: one and only one
for tx in (LX + 16, LX + 26):
    ax.plot([tx, tx], [-9, 9], color=RULE, linewidth=1.6, zorder=2)

# employee end: zero or many
ax.add_patch(Circle((RX - 30, 0), 5.0, facecolor="white", edgecolor=RULE,
                    linewidth=1.6, zorder=3))
for dy in (13, 0, -13):
    ax.plot([RX - 21, RX], [0, dy], color=RULE, linewidth=1.6, zorder=2)

ax.text((LX + RX) / 2.0, 22, "employs", ha="center", va="center",
        fontsize=10, style="italic", family=SANS, color=INK)
ax.text(LX + 21, -20, "1", ha="center", va="center", fontsize=8.8,
        family=SANS, color=SUBTLE)
ax.text(RX - 26, -20, "0..N", ha="center", va="center", fontsize=8.8,
        family=SANS, color=SUBTLE)

cx = content_w / 2.0
ax.text(cx, TITLE_Y, "Employee Manager - Database Schema", ha="center",
        va="center", fontsize=F_TITLE, fontweight="bold", family=SANS, color=INK)
ax.text(cx, SUB_Y, "MySQL 8 / InnoDB, utf8mb4  \u00b7  generated from V1__create_schema.sql",
        ha="center", va="center", fontsize=F_SUB, family=SANS, color=SUBTLE)
ax.text(cx, FK_Y, "fk_employee_department:  employee.department_id  \u2192  department.id",
        ha="center", va="center", fontsize=F_NOTE, family=MONO, color=INK)
ax.text(cx, NOTE1_Y, "One department has zero or many employees; every employee belongs "
        "to exactly one department (department_id is NOT NULL).",
        ha="center", va="center", fontsize=F_NOTE, family=SANS, color=SUBTLE)
ax.text(cx, NOTE2_Y, "PK = primary key    FK = foreign key    "
        "UNIQUE constraints: uq_department_name, uq_employee_email",
        ha="center", va="center", fontsize=F_NOTE, family=SANS, color=SUBTLE)

plt.savefig(sys.argv[1], dpi=300, facecolor="white")
print("wrote %s  (%.2f x %.2f in)" % (sys.argv[1], (x1 - x0) / 72.0, (y1 - y0) / 72.0))
